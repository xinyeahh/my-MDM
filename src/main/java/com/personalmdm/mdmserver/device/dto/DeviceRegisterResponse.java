package com.personalmdm.mdmserver.device.dto;

/**
 * 设备注册响应 DTO
    * record 是 Java 的一种数据类写法：编译器会生成构造方法和访问方法 deviceId()，适合这里简单、不可变的响应数据。
 */
public record DeviceRegisterResponse(String deviceId) {
}
