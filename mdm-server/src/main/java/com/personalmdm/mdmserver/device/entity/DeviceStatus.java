package com.personalmdm.mdmserver.device.entity;

/**
 * 设备状态
 * 枚举只能限制“有哪些状态”，不能限制“状态之间怎么变化”。状态变化是由业务逻辑负责的。
 */
public enum DeviceStatus {
    ACTIVE,     // 已激活
    REVOKE      // 已撤销
}
