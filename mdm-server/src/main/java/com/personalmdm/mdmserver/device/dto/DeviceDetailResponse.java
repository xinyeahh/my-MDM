package com.personalmdm.mdmserver.device.dto;

import java.time.LocalDateTime;

/**
 * 设备详情响应 DTO
 * @param deviceId      设备ID
 * @param deviceName    设备名称
 * @param model         设备型号
 * @param os            操作系统
 * @param osVersion     操作系统版本
 * @param status        设备状态
 * @param createTime    创建实践
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
