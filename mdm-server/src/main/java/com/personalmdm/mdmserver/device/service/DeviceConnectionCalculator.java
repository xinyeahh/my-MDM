package com.personalmdm.mdmserver.device.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component  // 让spring管理计算器
public class DeviceConnectionCalculator {
    public DeviceConnectionStatus calculate(
            LocalDateTime lastHeartbeatTime,
            LocalDateTime now
    ){
        // 从未收到过心跳-在线情况：未知
        if (lastHeartbeatTime == null){
            return DeviceConnectionStatus.UNKNOWN;
        }

        // 设备在线判断边界 2分钟
        LocalDateTime deadline = lastHeartbeatTime.plusMinutes(2);

        /**
         * 距离最近心跳超过 2分钟- 在线情况：下线
         * isAfter() 只在严格晚于截止时间时返回 true，因此恰好两分钟仍在线。
         */
        if (now.isAfter(deadline)) {
            return DeviceConnectionStatus.OFFLINE;
        }

        return DeviceConnectionStatus.ONLINE;
    }
}
