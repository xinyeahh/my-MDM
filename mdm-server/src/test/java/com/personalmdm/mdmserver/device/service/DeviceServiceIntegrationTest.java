package com.personalmdm.mdmserver.device.service;

import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.entity.DeviceStatusHistory;
import com.personalmdm.mdmserver.device.mapper.DeviceMapper;
import com.personalmdm.mdmserver.device.mapper.DeviceStatusHistoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证数据库事务是否真的回滚
 * 单独建类，是因为原测试类采用 Mockito 环境，新测试采用 Spring 和数据库环境，分开更容易维护。
 */
@SpringBootTest
@ActiveProfiles("test")
class DeviceServiceIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private DeviceStatusHistoryMapper deviceStatusHistoryMapper;

    /**
     * 设备撤销
     * 测试场景：撤销的数据库操作失败时的回滚
     */
    @Test
    void revokeRollsBackWhenHistoryInsertFails() {
        // 目标设备
        Device device = new Device();
        device.setDeviceId("dev_" + UUID.randomUUID());
        device.setFingerprint("test_" + UUID.randomUUID());
        device.setDeviceName("事务回滚测试设备");
        device.setModel("simulator-v1");
        device.setOs("Android");
        device.setOsVersion("15");
        device.setStatus(DeviceStatus.ACTIVE);

        // 记录目标设备业务ID，后面会频繁使用
        String deviceId = device.getDeviceId();

        try {
            // 准备真实数据库记录，此时尚未调用撤销事务，执行数据库插入
            assertEquals(1, deviceMapper.insert(device));

            // 预设准备插入历史的时候抛出异常
            when(deviceStatusHistoryMapper.insert(any(DeviceStatusHistory.class)))
                    .thenThrow(new DataIntegrityViolationException("模拟历史写入失败"));

            // Service 真实更新设备，但随后历史插入失败
            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> deviceService.revoke(deviceId)
            );

            // 方法抛出异常返回后，其事务应当已经回滚，设备的最新状态应该是：存在且状态保持激活
            Device after = deviceMapper.findByDeviceId(deviceId);
            assertNotNull(after);
            assertEquals(DeviceStatus.ACTIVE, after.getStatus());

            // 查询数据库的历史记录数量
            Long historyCount = jdbcTemplate.queryForObject(
                    "select count(*) from device_status_history where device_id = ?",
                    Long.class,
                    deviceId
            );

            // 校验历史记录数量应该为0，因为背景时历史写入失败
            assertEquals(Long.valueOf(0), historyCount);

            // 验证只执行一次历史写入
            verify(deviceStatusHistoryMapper).insert(any(DeviceStatusHistory.class));
        }finally {
            // 清理本条测试创建的数据，先清历史以满足外键约束
//            jdbcTemplate.update(
//                    "delete from device_status_history where device_id = ?",
//                    deviceId
//            );    按照测试的当前设置，历史不会写入，所以删除历史语句可以省略
            jdbcTemplate.update(
                    "delete from device where device_id = ?",
                    deviceId
            );
        }
    }
}
