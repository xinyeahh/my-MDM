package com.personalmdm.mdmserver.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 设备注册请求 DTO
 */
public class DeviceRegisterRequest {
    // 设备名称
    @NotBlank               // 校验注解-声明规则：要求字符串不为 null（没有值），并且不能只包含空白（空白是有字符串这个值，但字符串里面没有有效内容）
    @Size(max = 100)        // 校验注解-声明规则：限制长度，与数据库字段容量对应
    private String deviceName;

    // 设备型号
    @NotBlank
    @Size(max = 100)
    private String model;

    // 操作系统
    @NotBlank
    @Size(max = 50)
    private String os;

    // 操作系统版本
    @NotBlank
    @Size(max = 50)
    private String osVersion;

    // 设备指纹
    @NotBlank
    @Size(max = 128)
    private String fingerprint;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
