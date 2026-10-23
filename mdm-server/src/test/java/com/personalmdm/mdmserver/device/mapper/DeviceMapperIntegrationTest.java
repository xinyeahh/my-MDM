package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest             // 启动真实Spring应用上下文，默认不启动监听端口
@ActiveProfiles("test")     // 启动 application-test.properties 配置
public class DeviceMapperIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;  // 真正向MySQL执行查询

    @Autowired
    private DeviceMapper deviceMapper;

    @Test
    void connectsToTestDatabase() {
        String database = jdbcTemplate.queryForObject(
                "select database()",
                String.class
        );

        assertEquals("mdm_test", database);
    }

    /**
     * 测试 Mapper 是否能真正插入设备，并按指纹读取回来。
     * 这个测试覆盖了真实数据库行为：
     * - insert() 返回一条匹配结果。
     * - 数据库生成的主键回填到 device.id。
     * - 下划线列名映射到 Java 驼峰属性。
     * - 字符串状态转换成枚举。
     * - 数据库生成的创建时间能够读回。
     */
    @Test
    @Transactional      // 用于测试隔离：Spring 在测试开始时开启事务，默认在测试结束后回滚。因此测试内部能读到刚插入的数据，结束后这条记录不会保留。自增 ID 可能出现间隔，这是正常的。
    void insertedDeviceCanBeFoundByFingerprint() {
        Device device = new Device();
        device.setDeviceId("dev_"+ UUID.randomUUID());
        device.setFingerprint("test-" + UUID.randomUUID());
        device.setDeviceName("集成测试设备");
        device.setModel("simulator-v1");
        device.setOs("Android");
        device.setOsVersion("15");
        device.setStatus(DeviceStatus.ACTIVE);

        int rows = deviceMapper.insert(device);

        assertEquals(1, rows);
        assertNotNull(device.getId());

        Device found = deviceMapper.findByFingerprint(device.getFingerprint());

        assertNotNull(found);
        assertEquals(device.getId(), found.getId());
        assertEquals(device.getDeviceId(), found.getDeviceId());
        assertEquals(device.getDeviceName(), found.getDeviceName());
        assertEquals(device.getStatus(), found.getStatus());
        assertNotNull(found.getCreateTime());
    }

    /**
     * 验证数据库的 fingerprint 唯一约束
     * 插入两台设备：身份不同，但指纹相同，预期第二次插入被拒绝。
     */
    @Test
    @Transactional
    void duplicateFingerprintIsRejected() {
        String fingerprint = "test-" + UUID.randomUUID();

        Device first = new Device();
        first.setDeviceId("dev_" + UUID.randomUUID());
        first.setFingerprint(fingerprint);
        first.setDeviceName("第一台测试设备");
        first.setModel("simulator-v1");
        first.setOs("Android");
        first.setOsVersion("15");
        first.setStatus(DeviceStatus.ACTIVE);

        deviceMapper.insert(first);

        Device second = new Device();
        second.setDeviceId("dev_" + UUID.randomUUID());
        second.setFingerprint(fingerprint);
        second.setDeviceName("第二台测试设备");
        second.setModel("simulator-v2");
        second.setOs("Android");
        second.setOsVersion("15");
        second.setStatus(DeviceStatus.ACTIVE);

        assertThrows(
                DuplicateKeyException.class,
                () -> deviceMapper.insert(second)
        );
    }
}
