package com.personalmdm.mdmserver.device.dto;

import java.time.LocalDateTime;

/**
 * 设备详情响应 DTO
 */
public record DeviceDetailResponse(
        String deviceId,
        String deviceName,
        String model,
        String os,
        String osVersion,
        String status,
        LocalDateTime createTime
) {

}
