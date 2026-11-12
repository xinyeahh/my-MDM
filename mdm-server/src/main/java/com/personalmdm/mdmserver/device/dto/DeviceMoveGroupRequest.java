package com.personalmdm.mdmserver.device.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 设备移动设备组请求 DTO
 * @param groupId   目标设备组
 */
public record DeviceMoveGroupRequest(
        @NotNull    // 必须提供目标设备组
        @Positive   // 要求目标设备组ID大于0
        Long groupId
) {
}
