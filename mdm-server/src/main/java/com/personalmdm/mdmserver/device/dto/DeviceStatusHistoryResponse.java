package com.personalmdm.mdmserver.device.dto;

import com.personalmdm.mdmserver.device.entity.DeviceStatus;

import java.time.LocalDateTime;

/**
 * 设备状态变更历史响应 DTO
 * @param historyId     历史内部主键id
 * @param fromStatus    变更前状态
 * @param toStatus      变更后状态
 * @param occurredAt    变更时间
 */
public record DeviceStatusHistoryResponse(
        Long historyId,
        DeviceStatus fromStatus,
        DeviceStatus toStatus,
        LocalDateTime occurredAt
) {
}
