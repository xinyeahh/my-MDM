package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.entity.Device;
import org.apache.ibatis.annotations.*;

/**
 * Mapper：负责执行数据库操作的接口
 * 将这个接口标记为 MyBatis Mapper，由 MyBatis 提供实现
 */
@Mapper
public interface DeviceMapper {

    /**
     * 设备指纹查询设备
     * 用途：验证设备是否已注册成功（实现注册幂等性）
         * @Param("fingerprint")	为方法参数指定 SQL 中使用的名字
         * #{fingerprint}	使用参数绑定，把传入值作为数据交给数据库
     */
    @Select("""
            select id, device_id, fingerprint, device_name, model, os, os_version, status, create_time
            from device
            where fingerprint = #{fingerprint}  
            """)
    Device findByFingerprint(
            @Param("fingerprint")
            String fingerprint
    );

    /**
     * 查询fingerprint不存在时，生成deviceId，插入数据库，返回deviceId
         * #{deviceId} 等名称对应传入 Device 的属性。
         * 不插入 id 和 create_time，由数据库分别使用自增值和默认时间。
         * @Options 让 MyBatis 将数据库生成的主键写回 device.id；它不会同时回填 createTime。
         * 返回的 int 是受影响的行数，正常插入一条应为 1，并不是生成的主键。
     */
    @Insert("""
            insert into device (
                               device_id, fingerprint, device_name, model, os, os_version, status
            )
            values (
                   #{deviceId}, #{fingerprint}, #{deviceName}, #{model}, #{os}, #{osVersion}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")   //
    int insert(Device device);

    /**
     * 修改设备名称
     * 返回的是匹配行数，而不是实际修改行数
     */
    @Update("""
            update device
            set device_name = #{deviceName}
            where device_id = #{deviceId}
            """)
    int updateName(
            @Param("deviceName")
            String deviceName,
            @Param("deviceId")
            String deviceId
    );
}
