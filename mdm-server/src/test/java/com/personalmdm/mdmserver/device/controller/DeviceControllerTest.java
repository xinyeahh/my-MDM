package com.personalmdm.mdmserver.device.controller;

import com.personalmdm.mdmserver.device.dto.DeviceRegisterRequest;
import com.personalmdm.mdmserver.device.dto.DeviceRegisterResponse;
import com.personalmdm.mdmserver.device.dto.DeviceRenameRequest;
import com.personalmdm.mdmserver.device.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@WebMvcTest(DeviceController.class) // 加载Controller及相关MVC测试环境
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;    // 在进程内模拟HTTP请求，无须启动端口

    @MockitoBean    // 在Spring测试环境中提供模拟Service
    private DeviceService deviceService;

    /**
     * 设备注册：参数违法-设备名称只由空格组成
     *
     * @throws Exception
     */
    @Test
    void blankDeviceNameReturnsBadRequest() throws Exception {
        mockMvc.perform(
                post("/device/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceName": "   ",
                                  "model": "simulator-v1",
                                  "os": "Android",
                                  "osVersion": "15",
                                  "fingerprint": "install-validation"
                                }
                                """)
        ).andExpect(status().isBadRequest());   // 这句期待 400。没有校验时，请求会进入 Controller 并调用模拟 Service，当前代码通常返回 200，因此断言失败。

        verifyNoInteractions(deviceService);    // 检查Service完全没有被调用
    }

    /**
     * 设备注册：参数合法
     */
    @Test
    void validRegistrationReturnsDeviceId() throws Exception{
        when(deviceService.register(any(DeviceRegisterRequest.class))).thenReturn(new DeviceRegisterResponse("dev_test"));

        mockMvc.perform(
                post("/device/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "deviceName": "测试设备",
                                    "model": "simulator-v1",
                                    "os": "Android",
                                    "osVersion": "15",
                                    "fingerprint": "install-valid"
                                }
                                """)
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.deviceId")   // 表示读取响应 JSON 根对象中的 deviceId 字段。这里验证的是客户端实际收到的 JSON，而不是直接读取 Java 对象。
                        .value("dev_test")
                );

        // 捕获的是 Controller 传给 Service 的请求 DTO，用于检查 JSON 字段是否被正确解析。
        ArgumentCaptor<DeviceRegisterRequest> captor = ArgumentCaptor.forClass(DeviceRegisterRequest.class);

        verify(deviceService).register(captor.capture());

        DeviceRegisterRequest request = captor.getValue();

        assertEquals("测试设备", request.getDeviceName());
        assertEquals("simulator-v1", request.getModel());
        assertEquals("Android", request.getOs());
        assertEquals("15", request.getOsVersion());
        assertEquals("install-valid", request.getFingerprint());
    }


    /**
     * 设备注册
     * 测试 Service 抛出的异常能否转换成 HTTP 403
     */
    @Test
    void forbiddenRegistrationReturns403() throws Exception {
        when(deviceService.register(any(DeviceRegisterRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "设备已撤销，不能重新注册"
                ));

        mockMvc.perform(
                post("/device/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        . content("""
                                {
                                    "deviceName": "测试设备",
                                    "model": "simulator-v1",
                                    "os": "Android",
                                    "osVersion": "15",
                                    "fingerprint": "install-revoke"
                                }
                                """)
        ).andExpect(status().isForbidden());    // 期待403的响应
        verify(deviceService).register(any(DeviceRegisterRequest.class));
    }

    /**
     * 设备改名接口
     * 重点检查：路径中的设备ID和JSON中的新名称，是否正确传给了Service，以及成功时是否返回了204
     * @throws Exception
     */
    @Test
    void validRenameReturnsNocontent() throws Exception {
        mockMvc.perform(
                patch("/device/{deviceId}/name", "dev_existing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "deviceName": "新名称"
                                }
                                """)
        ).andExpect(status().isNoContent());

        // 捕获改名请求
        ArgumentCaptor<DeviceRenameRequest> captor = ArgumentCaptor.forClass(DeviceRenameRequest.class);

        verify(deviceService).rename(
                /**
                 * 表示参数必须等于这个设备 ID。
                 * 由于第二个参数用了 captor.capture() 这个 Mockito 匹配器，第一个参数也要使用匹配器，不能直接混写成普通字符串。
                 */
                eq("dev_existing"),
                captor.capture()
        );

        assertEquals("新名称", captor.getValue().getDeviceName());
    }

    /**
     * 设备重命名：设备新名称仅由空格组成，违法
     * @throws Exception
     */
    @Test
    void invalidRenameReturnsForbidden () throws Exception {
        mockMvc.perform(
                patch("/device/{deviceId}/name", "dev_existing")    // 路径中的{deviceId}需要替换值
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "deviceName": "    "
                                }
                                """)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(deviceService);
    }
}
