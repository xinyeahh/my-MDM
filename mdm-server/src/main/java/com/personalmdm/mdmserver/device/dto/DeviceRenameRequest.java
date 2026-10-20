package com.personalmdm.mdmserver.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新设备资料（设备名称）请求 DTO
 * 不复用注册 DTO，因为修改名称不需要客户端再次提交系统、型号和指纹。独立 DTO 能准确表达这个接口的输入边界。
 * 具有幂等性，幂等性体现在于执行一次和重复执行，最终存在数据库中的设备名称是相同的，也就是服务端的状态一致
 */
public class DeviceRenameRequest {
    @NotBlank
    @Size(max = 100)
    private String deviceName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
