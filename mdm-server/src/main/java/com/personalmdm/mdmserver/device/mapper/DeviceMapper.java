package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.dto.DeviceListItemResponse;
import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import org.apache.ibatis.annotations.*;

import java.util.List;

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
            select id, device_id, fingerprint, device_name, model, os, os_version, status, create_time, last_heartbeat_time
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
                               device_id, fingerprint, device_name, model, os, os_version, status, group_id
            )
            values (
                   #{deviceId}, #{fingerprint}, #{deviceName}, #{model}, #{os}, #{osVersion}, #{status}, #{groupId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")   // 主键回填
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

    /**
     * 设备ID查询设备
     * 用途：查询设备详情
     */
    @Select("""
            select device_id, device_name, model, os, os_version, status, create_time, last_heartbeat_time, group_id
            from device
            where device_id = #{deviceId}
            """)
    Device findByDeviceId(
            @Param("deviceId")
            String deviceId
    );

    /**
     * 查询设备总数
     */
    @Select("""
            select count(*)
            from device
            """)
    long countAll();




    /**
     * 查询设备总数
     * 注意：分页总数和列表必须使用相同的筛选条件
     * 查询条件：os
     * 动态sql
     * <script>	在注解中启用动态 SQL
     * <if>	条件成立时加入内部 SQL
     * <where>	内部有条件时才生成 WHERE
     */
    @Select("""
            <script>
            select count(*)
            from device
            <where>
                 <if test="os != null">
                    os = #{os}
                 </if>
            </where>
            </script>
            """)
    long countAllByOs(
            @Param("os")
            String os
    );

    /**
     * 撤销设备
     * 状态转换：激活->撤销
     */
    @Update("""
            update device
            set status = 'REVOKED'
            where device_id = #{deviceId} and status = 'ACTIVE'
            """)
    int revoke(
            @Param("deviceId")
            String deviceId
    );

    /**
     * 获取设备心跳
     * @param deviceId
     * @return
     */
    @Update("""
            update device
            set last_heartbeat_time = current_timestamp(6)
            where device_id=#{deviceId} and status='ACTIVE'
            """)
    int updateHeartbeat(@Param("deviceId") String deviceId);


    /**
     * 页码查询设备列表
     * 注意：分页总数和列表必须使用相同的筛选条件
     * 查询条件：os;status
     * 动态sql
     * <where> 会自动去掉开头多余的 AND，所以可以组合出四种查询：不筛选、只筛选系统、只筛选状态、同时筛选两者。
     */
    @Select("""
            <script>
            select id, device_id, device_name, model, os, os_version, status, create_time, last_heartbeat_time
            from device
            <where>
                <if test="os != null">
                    and os = #{os}
                </if>
                <if test="status != null">
                 and status = #{status}
                </if>
            </where>
            order by id desc
            limit #{size}
            offset #{offset}
            </script>
            """)
    List<Device> findPageByOsAndStatus(
            @Param("size")
            int size,
            @Param("offset")
            long offset,
            @Param("os")
            String os,
            @Param("status")
            DeviceStatus status
    );

    /**
     * 查询设备总数
     * 注意：分页总数和列表必须使用相同的筛选条件
     * 查询条件：os;status
     */
    @Select("""
            <script>
            select count(*)
            from device
            <where>
                <if test="os != null">
                    and os = #{os}
                </if>
                <if test="status != null">
                 and status = #{status}
                </if>
            </where>
            </script>
            """)
    long countAllByOsAndStatus(
            @Param("os")
            String os,
            @Param("status")
            DeviceStatus status
    );

    /**
     * 设备移动设备组
     * 设备处于激活状态才允许移动设备组
     */
    @Update("""
            update device
            set group_id = #{groupId}
            where device_id = #{deviceId} and status = 'ACTIVE'
            """)
    int updateDeviceGroup(
            @Param("groupId")
            Long groupId,   // 新设备组内部id
            @Param("deviceId")
            String deviceId // 要更新设备组的设备业务id
    );
}
