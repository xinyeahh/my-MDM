package com.personalmdm.mdmserver.device.dto;

import java.util.List;

/**
 * 设备列表分页响应DTO
 * @param page  页码
 * @param size  单次实际返回的条数
 * @param total 数据库中符合查询条件的设备总数
 * @param items 设备条目
 */
public record DeviceListPageResponse(
        int page,
        int size,
        long total,
        List<DeviceListItemResponse> items
) {
}
