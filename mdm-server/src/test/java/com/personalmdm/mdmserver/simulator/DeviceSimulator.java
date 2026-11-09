package com.personalmdm.mdmserver.simulator;


import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * 用来手动调用本地接口的小程序
 */
public class DeviceSimulator {
    public static void main(String[] args)
            throws IOException, InterruptedException {

        Path fingerprintFile = Path.of("simulator-fingerprint.txt");
        String fingerprint = loadOrCreateFingerprint(fingerprintFile);
        System.out.println("设备指纹：" + fingerprint);
        // ===== 准备数据 =====
        // 保存注册需要的设备信息
        String json =
                // """ 是java的文本块，便于直接写多行JSON
                """
                {
                    "deviceName": "学习用模拟设备",
                    "model": "simulator-v1",
                    "os": "Android",
                    "osVersion": "15",
                    "fingerprint": "%s"
                }
                """
                        // formatted 会用刚读到或者刚生成的指纹替换%s
                        .formatted(fingerprint);

        // ===== 组装请求 =====
        HttpRequest request = HttpRequest.newBuilder()
                // 指定发到什么地方
                .uri(URI.create("http://localhost:8080/device/register"))
                // 告诉服务端请求体是 JSON
                .header("Content-Type", "application/json")
                // 指定用 POST 发送这段 JSON
                .POST(HttpRequest.BodyPublishers.ofString(json))
                // 把这些设置组装成一个请求对象
                .build();

        // 发送并读取响应
        HttpClient client = HttpClient.newBuilder()
                // 给HttpClient设置连接超时
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        HttpResponse<String> response = client.send(    // client.send(...)真正发送请求
                // 请求
                request,
                // 让java把服务段返回的响应体读成字符串
                HttpResponse.BodyHandlers.ofString()
        );

        // 打印结果
        System.out.println("HTTP 状态码：" + response.statusCode());
        System.out.println("响应体：" + response.body());

        // 注册失败，直接返回
        if (response.statusCode() != 200) {
            return;
        }

        // 注册成功，打印设备 ID
        ObjectMapper mapper = new ObjectMapper();
        String deviceId =
                // readTree 把整段JSON解析成可以按字段读取的结构
                mapper.readTree(response.body())
                        // 找到字段
                        .path("deviceId")
                        // 取出字符值
                        .asText();
        System.out.println("设备 ID：" + deviceId);

        // 注册成功且设备ID也已取到
        if (deviceId.isBlank()) {
            System.out.println("注册响应缺少设备ID。停止发送心跳。");
            return;
        }

        String heartbeatUrl = "http://localhost:8080/device/" + deviceId + "/heartbeat";

        HttpRequest heartbeatRequest = HttpRequest.newBuilder()
                .uri(URI.create(heartbeatUrl))
                // 在构建心跳请求时设置请求超时
                .timeout(Duration.ofSeconds(3))
                .POST(
                        // 心痛接口直接从URL路径读取deviceId，所以使用noBody，无需发送json
                        HttpRequest.BodyPublishers.noBody()
                )
                .build();


        // 定时心跳
        for (int attempt = 1; attempt <= 3; attempt++){
            HttpResponse<Void> heartbeatResponse = sendHeartbeatWithRetry(client, heartbeatRequest);
            int statusCode = heartbeatResponse.statusCode();
            System.out.println("第 " + attempt + " 次心跳：" + heartbeatResponse.statusCode());

            // 如果心跳请求失败，则跳出循环
            if (statusCode != 204) {
                break;
            }

            // 第一次直接请求，第二、三次各在上一次成功后等待5秒
            if (attempt < 3) {
                Thread.sleep(5_000);
            }
        }
    }

    /**
     * 每一轮心跳，连接失败或超时时重试
     * @param client
     * @param request
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private static HttpResponse<Void> sendHeartbeatWithRetry(
            HttpClient client, HttpRequest request
    ) throws IOException, InterruptedException {
        int maxAttempts = 3;

        for (int attempt = 1; ; attempt++) {
            try {
                return client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (ConnectException | HttpTimeoutException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }

                System.out.println(
                        "心跳发送失败，1 秒后重试：" +
                        e.getClass().getSimpleName()
                );

                Thread.sleep(1_000);
            }
        }
    }

    /**
     * 让模拟器第一次启动时把指纹写入文件，第二次启动时读回同一个值。
     * @param file
     * @return
     * @throws IOException
     */
    private static String loadOrCreateFingerprint(Path file) throws IOException {
        if (Files.exists(file)) {
            return Files.readString(file).strip();
        }

        String fingerprint = "sim-" + UUID.randomUUID();
        Files.writeString(file, fingerprint);
        return fingerprint;
    }
}
