package com.personalmdm.mdmserver.device.entity;

import java.time.LocalDateTime;

/**
 * 数据库记录对应的Java对象：设备
 * */
public class Device {
    /**
     * 数据库内部主键
     * bigint对应Long
     */
    private Long id;

    /**
     * 设备业务标识
     * 代表设备注册记录的稳定身份
     */
    private String deviceId;

    /**
     * 设备指纹
     * 只要原记录仍保留，同一个值再次注册就复用原身份，不限于短时间内。
     * 客户端重新安装后如果生成了新值，当前方案会把它视为新注册。
     */
    private String fingerprint;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 操作系统版本
     */
    private String osVersion;

    /**
     * 设备状态
     */
    private DeviceStatus status;

    /**
     * 创建时间
     * LocalDateTime 表达不带时区的日期时间
     */
    private LocalDateTime createTime;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

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

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
