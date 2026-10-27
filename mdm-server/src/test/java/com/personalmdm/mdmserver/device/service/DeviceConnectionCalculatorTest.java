package com.personalmdm.mdmserver.device.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 设备连接状态计算器测试
 */
public class DeviceConnectionCalculatorTest {

    /**
     * 设备连接计算器：从未接受过设备心跳
     */
    @Test
    void missingHeartbeatReturnsUnknown() {
        // 声明设备连接计算器
        DeviceConnectionCalculator calculator = new DeviceConnectionCalculator();

        LocalDateTime now = LocalDateTime.of(
                2026, 10, 27, 10, 0
        );

        DeviceConnectionStatus result = calculator.calculate(null, now);

        assertEquals(DeviceConnectionStatus.UNKNOWN, result);

    }

    /**
     * 设备连接计算器：恰好两分钟时在线
     */
    @Test
    void exactlyTwoMinutesAfterHeartbeatIsOnline() {
        DeviceConnectionCalculator calculator = new DeviceConnectionCalculator();

        LocalDateTime heartbeat = LocalDateTime.of(
                2026, 10, 27, 10, 0
        );

        // 1. 用 heartbeat.plusMinutes(2) 得到当前时间
        LocalDateTime now = heartbeat.plusMinutes(2);

        // 2. 调用 calculate(heartbeat, now)
        DeviceConnectionStatus status = calculator.calculate(heartbeat, now);

        // 3. 断言结果为 ONLINE
        assertEquals(DeviceConnectionStatus.ONLINE, status);
    }

    /**
     * 设备连接计算器：超过两分钟就离线
     */
    @Test
    void moreThanTwoMinutesAfterHeartbeatIsOffline() {
        DeviceConnectionCalculator calculator = new DeviceConnectionCalculator();

        LocalDateTime heartbeat = LocalDateTime.of(
                2026, 10, 27, 10, 0
        );
        // 超过2分钟边界1纳秒
        LocalDateTime now = heartbeat.plusMinutes(2).plusNanos(1);
        DeviceConnectionStatus status = calculator.calculate(heartbeat, now);

        assertEquals(DeviceConnectionStatus.OFFLINE, status);
    }
}
