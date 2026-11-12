package com.personalmdm.mdmserver.device.entity;

import java.time.LocalDateTime;

/**
 * 设备历史状态
 */

public class DeviceStatusHistory {
    // 状态历史的主键
    private Long id;
    // 设备的业务ID
    private String deviceId;
    // 变更前状态
    private DeviceStatus fromStatus;
    // 变更后状态
    private DeviceStatus toStatus;
    // 变更时间
    private LocalDateTime occurredAt;

    public DeviceStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(DeviceStatus toStatus) {
        this.toStatus = toStatus;
    }

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

    public DeviceStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(DeviceStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
