package com.personalmdm.mdmserver.devicegroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建设备组请求 DTO
 * @param groupName 设备组名称
 */
public record DeviceGroupCreateRequest(
        // 设备组名称：非空
        @NotBlank
        @Size(max = 100)
        String groupName
) {
}