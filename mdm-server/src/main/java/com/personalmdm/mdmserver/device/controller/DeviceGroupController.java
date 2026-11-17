package com.personalmdm.mdmserver.device.controller;

import com.personalmdm.mdmserver.device.dto.DeviceGroupCreateRequest;
import com.personalmdm.mdmserver.device.dto.DeviceGroupCreateResponse;
import com.personalmdm.mdmserver.device.service.DeviceConnectionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device-group")
public class DeviceGroupController {
    // 声明依赖
    private final DeviceConnectionStatus.DeviceGroupService deviceGroupService;

    // 构造注入
    public DeviceGroupController(DeviceConnectionStatus.DeviceGroupService deviceGroupService){
        this.deviceGroupService = deviceGroupService;
    }

    /**
     * 创建设备组
     * @param request
     * @return
     */
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)     // 期待返回创建成功状态
    public DeviceGroupCreateResponse create(
            @Valid
            @RequestBody
            DeviceGroupCreateRequest request
    ){
        return deviceGroupService.create(request);
    }

}

