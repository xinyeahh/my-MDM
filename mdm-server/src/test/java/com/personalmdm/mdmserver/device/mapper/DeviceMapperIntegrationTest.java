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


}
