// 测试类位置
package com.personalmdm.mdmserver.device.service;

// 测试所需的类
import com.personalmdm.mdmserver.device.dto.DeviceRegisterRequest;
import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// 静态导入：允许直接使用方法，不用每次都写完整的类名
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Mockito 是 Java 中非常常用的单元测试模拟框架。
 * - 核心思想：模拟依赖
 *      - DeviceService：使用真实对象，测试它自己的逻辑。
 *      - DeviceMapper：使用 Mockito 创建的模拟对象。
 *      - MySQL：这次测试不需要连接。
 * - 它可以让你在测试代码时，模拟其他类的行为，而不必真的连接数据库、调用远程服务或启动整个系统。
 * - Mockito 可以让你单独测试 DeviceService，而不必真的运行 MySQL 和 DeviceMapper。
 */

/**
 * @ExtendWith(MockitoExtension.class)主要负责：
 * - 告知JUnit 5 运行这个测试类时，使用 Mockito 提供的扩展。
 * - 初始化 Mockito 的注解，例如 @Mock，并管理相应的测试生命周期。
 * - 如果缺少这个扩展，又没有采用其他初始化方式，deviceMapper 就可能没有被正确初始化。
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock   // 创建模拟的 DeviceMapper 对象，而非连接数据库的真实Mapper
    private DeviceMapper deviceMapper;

    /**
     * 撤销设备不允许重新注册
     * @Test 表示这是一个可以由JUnit执行的测试方法
     * 方法名采用行为描述式命名，表达了一个业务规则
     *
     * 单元测试结构：准备->执行->断言
     */
    @Test
    void revokeDeviceCannotRegisterAgain(){
        /**
         * 准备：模拟数据库中已有一台撤销设备。
         * 创建一个Device对象，并设置两个属性
         * 并未保存到数据库
         */
        Device existing = new Device();             // 表示数据库查询后发现了什么
        existing.setDeviceId("dev_test");           // 设备业务标识
        existing.setStatus(DeviceStatus.REVOKE);    // 设备已撤销

        /**
         * 创建注册请求，并设置设备指纹
         * - 是测试数据，用于模拟某次设备注册请求。
         * - 它不是真实设备身份，也不需要真的对应数据库记录。
         */

        DeviceRegisterRequest request = new DeviceRegisterRequest();    // 设备提交了什么
        request.setFingerprint("install-test");

        /**
         * 预设查询结果：
         * 当 Service 调用 deviceMapper.findByFingerprint("install-test") 时，让模拟的 Mapper 返回前面构造的 existing 对象。
         */
        when(deviceMapper.findByFingerprint("install-test")).thenReturn(existing);

        /**
         * 代码通过构造器创建一个真实的 DeviceService 对象，并把模拟的 Mapper 传进去
         * 创建完成后：
         * - service 是真实的 DeviceService。
         * - service 内部的 deviceMapper 是 Mockito 模拟对象。
         * 这种方式叫手动构造测试对象：
         * - 因为这个测试只验证 Service 的业务逻辑，所以不必启动完整的 Spring Boot 容器。
         * 构造器注入有利于单元测试：
         * - 依赖可以很方便地替换为模拟对象
         */
        DeviceService deviceService = new DeviceService(deviceMapper);

        /**
         * 执行：调用注册方法，并断言它抛出异常。
         * 异常测试：
         * assertThrows() 是 JUnit 提供的断言方法，用于验证一段代码是否抛出了指定类型的异常。
         *      - 预期异常类型是 ResponseStatusException。
         *      - 执行设备注册方法，并确认它抛出了 ResponseStatusException。
         *      - 如果方法没有抛出异常，测试就会失败。
         *      - 如果抛出的是不兼容的异常类型，测试也会失败。
         *
         * 把异常赋值给 exception 不仅会检查异常是否发生，还会返回捕获到的异常对象。
         * 这样就可以进一步检查：
         * - 异常对应的 HTTP 状态码。
         * - 异常消息。
         * - 其他需要验证的异常信息。
         * 否则只能验证指定类型的异常是否发生
         */
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                /**
                 * 是一种 Lambda 表达式：把这段代码交给 assertThrows()，让它在执行断言时调用。
                 * 如果没有 () -> ，编译器会将其理解为直接调用，方法会在assertThrows()执行之前就调用，无法按预期捕获和判断异常
                 */
                () -> deviceService.register(request)
        );

        /**
         * 检查：异常状态为 403，且没有尝试插入新设备。
         * JUnit 的 assertEquals()，验证实际值是否等于预期值。
         * 当已撤销设备尝试重新注册时，Service 不仅要拒绝请求，而且应该通过约定的异常类型返回 403 对应的状态。
         */
        assertEquals(
                HttpStatus.FORBIDDEN,       //预期状态码
                exception.getStatusCode()   // 获取捕获到的 ResponseStatusException 中携带的 HTTP 状态码。
        );

        /**
         * 验证 deviceMapper.insert() 一次都没有被调用过，而且传入参数属于 Device 类型。
         * 因为目标是返回 403
         */
        verify(                     // 验证调用行为：用于检查模拟对象是否发生了预期的交互
                deviceMapper,
                never()             // 表示指定的方法在测试过程中不能被调用。
        ).insert(
                any(Device.class)   // Mockito 提供的参数匹配器，用于匹配非 null 的 Device 类型参数。
        );


    }

    /**
     * 并发冲突后的分支也会拒绝已撤销设备
     */
    @Test
    void revokeDeviceFoundAfterDuplicateKeyCannotRegister(){
        // 准备：模拟数据库中的一台设备
        Device existing = new Device();
        existing.setDeviceId("dev_test");
        existing.setStatus(DeviceStatus.REVOKE);

        // 准备：注册请求
        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setFingerprint("install-test");

        // 准备：预设查询结果
        // 第一次查询没有记录，成功注册
        // 表示同一个查询方法第一次调用返回 null，第二次调用返回 existing
        when(deviceMapper.findByFingerprint("install-test")).thenReturn(null, existing);

        // 尝试插入，发生唯一键冲突
        when(deviceMapper.insert(any(Device.class))).thenThrow(new DuplicateKeyException("模拟唯一键冲突"));

        // 准备：手动构造测试对象
        DeviceService deviceService = new DeviceService(deviceMapper);

        // 执行代码
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> deviceService.register(request)
        );

        // 检查结果
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(
                deviceMapper,
                times(2)       // 执行两次查询
        ).findByFingerprint("install-test");
        verify(deviceMapper).insert(any(Device.class));
    }
}
