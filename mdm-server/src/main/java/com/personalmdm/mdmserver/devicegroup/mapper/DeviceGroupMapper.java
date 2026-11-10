package com.personalmdm.mdmserver.devicegroup.mapper;

import com.personalmdm.mdmserver.devicegroup.entity.DeviceGroup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface DeviceGroupMapper {

    @Insert("""
            insert into device_group (group_name)
            values (#{groupName})
            """)
    // 让 myBatis 将数据库生成的主键写回 device.id
    @Options(

            useGeneratedKeys = true,
            keyProperty = "id"
    )
    int insert(DeviceGroup group);
}
