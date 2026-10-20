package com.personalmdm.mdmserver.device.controller;

import com.personalmdm.mdmserver.device.dto.DeviceRegisterRequest;
import com.personalmdm.mdmserver.device.dto.DeviceRegisterResponse;
import com.personalmdm.mdmserver.device.dto.DeviceRenameRequest;
import com.personalmdm.mdmserver.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @RestController：将类声明为处理 HTTP 请求的控制器，返回对象会被序列化为响应数据
 * @RequestMapping("/device")：设置这个控制器的公共路径
 * @PostMapping("/register")：接收 POST /device/register
 * @RequestBody：将请求中的JSON转成请求DTO
 * @Valid：触发DTO中的 @NotBlank、@Size 校验
 */
@RestController                 // 将类声明为处理 HTTP 请求的控制器，返回对象会被序列化为响应数据
@RequestMapping("/device")   // 设置这个控制器的公共路径
public class DeviceController {

    /**
     * 声明依赖
     * DeviceController 正常运行时就应该依赖一个确定的 Service。使用 final 能明确表达这个依赖是必需的，不应该在对象创建后随意更换。
     */
    private final DeviceService deviceService;

    /**
     * 构造方法
     * 当 Spring 扫描到这个 Controller，并且容器中存在可注入的 DeviceService Bean 时，Spring 就可以调用这个构造器，把 Service 传进来。
     * @param deviceService
     */
    public DeviceController(DeviceService deviceService){
        this.deviceService = deviceService;
    }

    /**
     * 设备注册
     * @param request
     * @return
     */
    @PostMapping("/register")   // 接收 POST /device/register
    public DeviceRegisterResponse register(
            @Valid          // 触发 DTO 中的 @NotBlank、@Size 校验
            @RequestBody    // 将请求中的 JSON 转成请求 DTO
            DeviceRegisterRequest request
    ){
        return deviceService.register(request);
    }

    @PatchMapping("/{deviceId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 指定接口执行后返回 HTTP 204 状态码，表示请求成功处理，但没有响应正文。
    public void rename(
            @Valid
            @RequestBody
            DeviceRenameRequest request,
            @PathVariable("deviceId")   // 把路径中的设备 ID 传给方法参数
            String deviceId
    ){
        deviceService.rename(deviceId,request);
    }
}
