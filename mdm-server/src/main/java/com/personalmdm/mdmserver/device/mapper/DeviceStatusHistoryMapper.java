package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.entity.DeviceStatusHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeviceStatusHistoryMapper {

    /**
     * 插入 状态变更历史记录
     * @param history   状态变更历史记录
     * @return          插入结果（变更行数）
     */
    @Insert("""
            insert into device_status_history (device_id, from_status, to_status, occurred_at) 
            values (#{deviceId}, #{fromStatus}, #{toStatus}, #{occurredAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")   // 主键回填
    int insert(DeviceStatusHistory history);

    /**
     * 依据设备业务ID查询设备的状态变更历史记录
     * @param deviceId  设备业务ID
     * @return          设备状态变更历史列表
     */
    @Select("""
            select id, device_id, from_status, to_status, occurred_at
            from device_status_history
            where device_id = #{deviceId}
            order by occurred_at desc, id desc
            """)
    List<DeviceStatusHistory> findByDeviceId(
            @Param("deviceId")
            String deviceId
    );
}
