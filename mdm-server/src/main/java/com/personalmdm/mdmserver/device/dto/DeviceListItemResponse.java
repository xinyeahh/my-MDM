package com.personalmdm.mdmserver.device.dto;

import com.personalmdm.mdmserver.device.entity.DeviceStatus;

/**
 * 设备列表条目响应DTO
 * 列表只展示概要，详细字段仍然通过详情接口获取。
 * @param deviceId      设备ID
 * @param deviceName    设备名称
 * @param model         设备型号
 * @param os            操作系统
 * @param status        设备状态
 */
public record DeviceListItemResponse(
        String deviceId,
        String deviceName,
        String model,
        String os,
        String status
) {
}
