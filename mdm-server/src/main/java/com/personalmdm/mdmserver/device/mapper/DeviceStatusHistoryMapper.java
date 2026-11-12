package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.entity.DeviceStatusHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface DeviceStatusHistoryMapper {

    @Insert("""
            insert into device_status_history (device_id, from_status, to_status, occurred_at) 
            values (#{deviceId}, #{fromStatus}, #{toStatus}, #{occurredAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")   // 主键回填
    int insert(DeviceStatusHistory history);
}
