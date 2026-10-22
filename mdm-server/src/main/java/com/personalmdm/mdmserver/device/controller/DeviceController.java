package com.personalmdm.mdmserver.device.controller;

import com.personalmdm.mdmserver.device.dto.*;
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
     * @param deviceService 服务
     */
    public DeviceController(DeviceService deviceService){
        this.deviceService = deviceService;
    }

    /**
     * 设备注册
     * @param request   设备注册请求 DTO
     * @return          设备注册响应 DTO
     */
    @PostMapping("/register")   // 接收 POST /device/register
    public DeviceRegisterResponse register(
            @Valid          // 触发 DTO 中的 @NotBlank、@Size 校验
            @RequestBody    // 将请求中的 JSON 转成请求 DTO
            DeviceRegisterRequest request
    ){
        return deviceService.register(request);
    }

    /**
     * 设备修改名称
     * @param request   修改名称请求 DTO
     * @param deviceId  设备 ID
     */
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

    /**
     * 查询设备详情
     * @param deviceId 设备 ID
     * @return         设备详情响应DTO
     */
    @GetMapping("/{deviceId}")
    public DeviceDetailResponse getDetail(
            @PathVariable("deviceId")
            String deviceId
    ){
        return deviceService.getDetail(deviceId);
    }

    /**
     * 分页查询设备列表
     * @param page  页码
     * @param size  单次返回的设备数
     * @param os    查询条件：操作系统
     * @return      设备列表页面响应 DTO
     */
    @GetMapping     // 没有子路径的 @GetMapping 与类上的 /device 组合，对应 GET /device
    public DeviceListPageResponse list(
            @RequestParam(name = "page", defaultValue = "1")    // @RequestParam 读取 URL 中 ? 后的参数 GET /device?page=2&size=10
            int page,
            @RequestParam(name = "size", defaultValue = "10")
            int size,
            @RequestParam(name = "os", required = false)    // required = false 表示参数可以省略，省略时这里收到 null。
            String os
    ){
        return deviceService.list(page, size, os);
    }

    /**
     * 设备撤销
     * 撤销成功返回 204
     * @param deviceId 设备ID
     */
    @PostMapping("/{deviceId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)      // 设备撤销成功时，返回204
    public void revoke(
            @PathVariable("deviceId")
            String deviceId
    ){
        deviceService.revoke(deviceId);
    }

}
