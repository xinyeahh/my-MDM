package com.personalmdm.mdmserver.device.dto;

import java.time.LocalDateTime;

/**
 * 设备详情响应 DTO
 * @param deviceId          设备ID
 * @param deviceName        设备名称
 * @param model             设备型号
 * @param os                操作系统
 * @param osVersion         操作系统版本
 * @param status            设备状态
 * @param createTime        创建时间
 * @param lastHeartbeatTime 设备最近一次心跳时间
 * @param connectionStatus  设备在线状态
 * @param groupId          设备组ID（外键）
 */
public record DeviceDetailResponse(
        String deviceId,
        String deviceName,
        String model,
        String os,
        String osVersion,
        String status,
        LocalDateTime createTime,
        LocalDateTime lastHeartbeatTime,
        String connectionStatus,
        Long groupId
) {

}
