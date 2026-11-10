package com.personalmdm.mdmserver.devicegroup.dto;

/**
 * 创建设备组响应 DTO
 * @param groupId   设备组ID
 * @param groupName 设备组名称
 */
public record DeviceGroupCreateResponse(
        Long groupId,
        String groupName
) {
}
