package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.entity.DeviceStatusHistory;
import com.personalmdm.mdmserver.device.service.DeviceService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

/**
 * 撤销成功后历史记录也成功写入
 */

@SpringBootTest
@ActiveProfiles("test")     // 运行这个测试类时，启用名为 test 的配置环境，项目运行时Spring会现读取 application.properties ，再读取 application-test.properties。同名配置由后者覆盖。
@Transactional
class DeviceStatusHistoryIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void revokePersistsHistoryOnlyOnce() {
        // 1. 创建并插入一台ACTIVE设备，ID和指纹使用UUID
        Device device = new Device();
        device.setDeviceId("dev_" + UUID.randomUUID());
        device.setFingerprint("test_" + UUID.randomUUID());
        device.setDeviceName("撤销成功并成功写入日志测试");
        device.setModel("OPPO-reno");
        device.setOs("Android");
        device.setOsVersion("15");
        device.setStatus(DeviceStatus.ACTIVE);

        String deviceId = device.getDeviceId();

        deviceMapper.insert(device);

        // 2. 首次撤销
        deviceService.revoke(deviceId);

        // 3. 重查设备，断言状态为REVOKED
        Device latest = deviceMapper.findByDeviceId(deviceId);
        assertNotNull(latest);
        assertEquals(DeviceStatus.REVOKED, latest.getStatus());

        // 4. 查询历史，断言只有一条，且为ACTIVE -> REVOKED
        Long historyRows = jdbcTemplate.queryForObject(
                "select count(*) from device_status_history where device_id = ?",
                Long.class,
                deviceId
        );

        assertEquals(Long.valueOf("1"), historyRows);

//        DeviceStatusHistory history = jdbcTemplate.queryForObject(
//                "select * from device_status_history where device_id = ?",
//                DeviceStatusHistory.class,
//                deviceId
//        );    Class 参数适合读取单列值，不能自动把多列结果组装为实体。

        String fromStatus = jdbcTemplate.queryForObject(
                "select from_status from device_status_history where device_id = ?",
                String.class,
                deviceId
        );
        String toStatus = jdbcTemplate.queryForObject(
                "select to_status from device_status_history where device_id = ?",
                String.class,
                deviceId
        );
        assertEquals("ACTIVE", fromStatus);
        assertEquals("REVOKED", toStatus);

        // 5. 再次撤销
        deviceService.revoke(deviceId);

        // 6. 再查历史数量，断言仍为 1
        Long historyLatestRows = jdbcTemplate.queryForObject(
                "select count(*) from device_status_history where device_id = ?",
                Long.class,
                deviceId
        );

        assertEquals(Long.valueOf("1"), historyLatestRows);
    }

}
