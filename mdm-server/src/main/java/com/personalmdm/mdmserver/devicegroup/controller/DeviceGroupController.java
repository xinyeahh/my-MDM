package com.personalmdm.mdmserver.devicegroup.controller;

import com.personalmdm.mdmserver.devicegroup.dto.DeviceGroupCreateRequest;
import com.personalmdm.mdmserver.devicegroup.dto.DeviceGroupCreateResponse;
import com.personalmdm.mdmserver.devicegroup.service.DeviceGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device-group")
public class DeviceGroupController {
    // 声明依赖
    private final DeviceGroupService deviceGroupService;

    // 构造注入
    public DeviceGroupController(DeviceGroupService deviceGroupService){
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

