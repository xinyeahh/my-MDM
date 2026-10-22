package com.personalmdm.mdmserver.device.controller;

import com.personalmdm.mdmserver.device.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(DeviceController.class) // 加载Controller及相关MVC测试环境
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;    // 在进程内模拟HTTP请求，无须启动端口

    @MockitoBean    // 在Spring测试环境中提供模拟Service
    private DeviceService deviceService;

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
}
