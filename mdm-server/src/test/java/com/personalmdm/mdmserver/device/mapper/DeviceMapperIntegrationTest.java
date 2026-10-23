package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
