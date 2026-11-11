package com.personalmdm.mdmserver.devicegroup.mapper;

import com.personalmdm.mdmserver.devicegroup.entity.DeviceGroup;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DeviceGroupMapper {

    /**
     * 创建设备组
     * @param group 插入设备组
     * @return
     */
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

    /**
     * 设备组名称是否已存在检查
     * @param groupName
     * @return
     */
    @Select("""
            select id, group_name
            from device_group
            where group_name = #{groupName}
            """)
    DeviceGroup findByGroupName(
            @Param("groupName")
            String groupName
    );

    /**
     * 按设备组id查询设备
     */
    @Select("""
            select id, group_name
            from device_group
            where id = #{id}
            """)
    DeviceGroup findById(
            @Param("id")
            Long id
    );
}
