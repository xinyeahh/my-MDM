package com.personalmdm.mdmserver.device.controller;

import com.personalmdm.mdmserver.device.dto.*;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
     * 设备注册接口：输入违法400
     * 被注解为@NotBlank的参数，输入空格
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
     * 设备注册接口：参数合法200
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
     * 设备注册接口：设备已撤销 403
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
     * 设备改名接口：成功204
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
     * 设备改名接口：输入违法400
     * @throws Exception
     */
    @Test
    void invalidRenameReturnsForbidden () throws Exception {
        mockMvc.perform(
                /**
                 * 路径中的{deviceId}需要替换值，这里传入 "dev_existing" 只是为了构造完整请求路径，不需要数据库中真的有这台设备
                 * 因为参数校验会先拒绝请求，模拟 Service 不会被调用。
                 */
                patch("/device/{deviceId}/name", "dev_existing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "deviceName": "    "
                                }
                                """)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(deviceService);
    }

    /**
     * 设备改名接口：设备不存在404
     * Service判断设备不存在，Controller返回404
     * @throws Exception
     */
    @Test
    void renamingMissingDeviceReturnsNotFound() throws Exception {
        /**
         * void 方法抛异常的写法：doThrow(异常).when(模拟对象).方法(参数);
         *
         * 因为 rename() 返回 void，无法把它作为值传给 when(...)。Mockito 因此提供了 doThrow(...).when(...) 这种写法。
         */
        doThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "设备不存在"
        ))
                .when(deviceService)
                .rename(
                        eq("dev_missing"),
                        any(DeviceRenameRequest.class)
                );

        mockMvc.perform(
                patch("/device/{deviceId}/name", "dev_missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "deviceName": "新名称"
                                }
                                """)
        ).andExpect(status().isNotFound());

        verify(deviceService).rename(
                eq("dev_missing"),
                any(DeviceRenameRequest.class)
        );
    }

    /**
     * 设备详情查询接口：成功200
     * @throws Exception
     */
    @Test
    void existingDeviceReturnsDetail() throws Exception {
        DeviceDetailResponse detail = new DeviceDetailResponse(
                "dev_existing",
                "测试设备",
                "simulator-v1",
                "Android",
                "15",
               "ACTIVE",
                LocalDateTime.of(2026, 10, 23, 10, 0)
        );

        // 1. 模拟 getDetail("dev_existing") 返回 detail
        when(deviceService.getDetail("dev_existing")).thenReturn(detail);

        // 2. 使用 mockMvc 发送 GET /device/dev_existing
        mockMvc.perform(
                get("/device/{deviceId}", "dev_existing")
        )
                // 3. 验证 HTTP 200
                .andExpect(status().isOk())
                /**
                 * 4. 验证 JSON 中：
                 *  - deviceId 为 dev_existing
                 *  - deviceName 为 测试设备
                 *  - status 为 ACTIVE
                 *  jsonPath(...)：检查返回给客户端的数据。
                 *  每个 .andExpect(...) 增加一项检查，可以连续写多个，最后才用分号结束。
                 */
                .andExpect(jsonPath("$.deviceId").value("dev_existing"))
                .andExpect(jsonPath("$.deviceName").value("测试设备"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        /**
         * 5. 验证 Service 收到了正确的设备 ID
         * verify(...)：检查传给 Service 的参数。
         */
        verify(deviceService).getDetail("dev_existing");
    }


    /**
     * 设备详情接口：设备不存在 404
     * @throws Exception
     */
    @Test
    void missingDeviceReturnNotFound() throws Exception {
        // 1. 模拟 getDetail("dev_missing")
        //    抛出 ResponseStatusException，状态为 NOT_FOUND
        when(deviceService.getDetail("dev_missing"))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "设备不存在"
                        )
                );

        // 2. 用 mockMvc 发送 GET 请求，设备 ID 放在路径中
        mockMvc.perform(
                get("/device/{deviceId}", "dev_missing")
        )
                // 3. 检查 HTTP 状态为 404
                .andExpect(status().isNotFound());
        // 4. 验证 Service 收到了 dev_missing
        verify(deviceService).getDetail("dev_missing");
    }


    /**
     * 测试分页查询列表接口：验证默认参数是否正确传给 Service
     */
    @Test
    void listingDevicesUsesDefaultParameters() throws Exception {
        DeviceListPageResponse page = new DeviceListPageResponse(
                1,
                10,
                0L,
                List.of()
        );

        // 1. 模拟 list(1, 10, null) 返回 page
        when(deviceService.list(1, 10, null))
                .thenReturn(page);

        // 2. 发送 GET /device
        mockMvc.perform(
                get("/device")  // 因为是验证默认参数是否传入，所以此处不传查询参数
        )
                // 3. 检查 HTTP 200
                .andExpect(status().isOk())
                // 4. JSON 中 page 为 1、size 为 10、total 为 0
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").value(0))
                // 5. items 是空数组
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());


        // 6. 验证 Service 收到 list(1, 10, null)
        verify(deviceService).list(1,10,null);
    }

}
