// 测试类位置
package com.personalmdm.mdmserver.device.service;

// 测试所需的类
import com.personalmdm.mdmserver.device.dto.DeviceListPageResponse;
import com.personalmdm.mdmserver.device.dto.DeviceRegisterRequest;
import com.personalmdm.mdmserver.device.dto.DeviceRegisterResponse;
import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// 静态导入：允许直接使用方法，不用每次都写完整的类名
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
     * 注册
     * 撤销设备不允许重新注册
     *
     * @Test 表示这是一个可以由JUnit执行的测试方法
     * 方法名采用行为描述式命名，表达了一个业务规则
     * <p>
     * 单元测试结构：准备->执行->断言
     */
    @Test
    void revokeDeviceCannotRegisterAgain() {
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
     * 注册
     * 并发冲突后的分支也会拒绝已撤销设备
     */
    @Test
    void revokeDeviceFoundAfterDuplicateKeyCannotRegister() {
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
                times(2)       // 验证查询执行了两次
        ).findByFingerprint("install-test");
        verify(deviceMapper).insert(any(Device.class));
    }

    /**
     * 注册
     * 已激活设备重复注册，返回原来的 deviceId，并且不插入新记录。
     * 准备：Mapper 返回 ACTIVE 设备
     * 执行：调用 register()
     * 检查：返回原 ID，未调用 insert()
     */
    @Test
    void activeDeviceRegisterAgainWithSameId() {
        // 准备：模拟数据库中已存在的一个设备
        Device existing = new Device();
        existing.setDeviceId("dev_existing");
        existing.setStatus(DeviceStatus.ACTIVE);

        // 准备：设备注册请求
        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setFingerprint("install-active");

        // 准备：预设查询结果
        when(deviceMapper.findByFingerprint("install-active")).thenReturn(existing);

        // 准备：构造注入mapper
        DeviceService deviceService = new DeviceService(deviceMapper);

        // 执行：调用注册方法，保存返回的响应 DTO
        DeviceRegisterResponse response = deviceService.register(request);

        // 验证重复注册返回已有身份
        assertEquals("dev_existing", response.deviceId());

        // 验证重复注册返回已有身份。
        verify(deviceMapper, never()).insert(any(Device.class));
    }

    /**
     * 注册
     * 首次注册成功
     * 查询指纹 → 不存在 → 创建设备 → 插入 → 返回新 deviceId
     */
    @Test
    void newDeviceRegistersSuccessfully() {
        // 准备：新设备注册请求
        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setFingerprint("install-new");
        request.setDeviceName("测试设备");
        request.setModel("simulator-v1");
        request.setOs("Android");
        request.setOsVersion("15");

        // 准备：预设查询结果
        when(deviceMapper.findByFingerprint("install-new")).thenReturn(null);
        when(deviceMapper.insert(any(Device.class))).thenReturn(1);

        // 准备：构造注入
        DeviceService deviceService = new DeviceService(deviceMapper);

        // 执行
        DeviceRegisterResponse response = deviceService.register(request);

        // 捕获实际传给insert的对象
        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);

        // 找到service调用insert()时传入的对象，取出它，再检查状态
        verify(deviceMapper).insert(captor.capture());
        Device saved = captor.getValue();   // saved 只是变量名；模拟 Mapper 没有真的把它写入数据库。

        // 检查
        assertEquals(DeviceStatus.ACTIVE, saved.getStatus());
        assertEquals("install-new", saved.getFingerprint());
        assertEquals("测试设备", saved.getDeviceName());
        assertEquals("simulator-v1", saved.getModel());
        assertEquals("Android", saved.getOs());
        assertEquals("15", saved.getOsVersion());

        // 因为设备id是随机生成的，所以这里只检查是否为空
        assertNotNull(saved.getDeviceId());
        assertTrue(saved.getDeviceId().startsWith("dev_"));
        assertEquals(saved.getDeviceId(), response.deviceId());
    }

    /**
     * 注册
     * 插入发生唯一键冲突，但再次按指纹查询仍找不到设备时，应该保留原异常。
     * 因为冲突也可能来自device_id，不能把所有唯一键冲突都当成“重复设备注册成功”。
     */
    @Test
    void duplicateKeyWithoutExistingDeviceRethrowOriginalException() {
        // 准备：设备注册请求
        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setFingerprint("install-conflict");

        // 准备：预设查询结果为空
        when(deviceMapper.findByFingerprint("install-conflict")).thenReturn(null);  // 该查询每次都返回 null

        // 准备：模拟唯一键冲突
        DuplicateKeyException oringinal = new DuplicateKeyException("模拟无法恢复的唯一键冲突");

        // 准备：预设插入时抛出唯一键冲突
        when(deviceMapper.insert(any(Device.class))).thenThrow(oringinal);

        // 准备：注入构造
        DeviceService deviceService = new DeviceService(deviceMapper);

        // 执行：使用actual保存代码执行过程中捕获的唯一键冲突
        DuplicateKeyException actual = assertThrows(DuplicateKeyException.class, () -> deviceService.register(request));

        /**
         * assertEquals 检查值是否相等
         * assertSame 检查是否为同一个对象。
         */
        assertSame(oringinal, actual);   // 检查抛出的就是原来的那个异常对象，对应service中的 `throw e;`

        // 验证是否执行两次查询
        verify(
                deviceMapper,
                times(2)    // 验证 findByFingerprint() 方法恰好被调用了两次
        ).findByFingerprint("install-conflict");
    }

    /**
     * 注册
     * 插入发生冲突后，查到了已激活设备，应返回已有身份
     */
    @Test
    void activeDeviceFoundAfterDuplicateKeyReturnsExistingId() {
        // 1. 准备已有设备：deviceId 为 dev_existing，状态为 ACTIVE
        Device existing = new Device();
        existing.setDeviceId("dev_existing");
        existing.setStatus(DeviceStatus.ACTIVE);

        // 2. 准备请求：fingerprint 为 install-race
        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setFingerprint("install-race");

        // 3. 模拟第一次查询返回 null，第二次返回已有设备
        when(deviceMapper.findByFingerprint("install-race")).thenReturn(null, existing);

        // 4. 模拟 insert 抛出 DuplicateKeyException
        when(deviceMapper.insert(any(Device.class))).thenThrow(DuplicateKeyException.class);

        // 5. 创建 Service，调用 register，取得响应
        DeviceService deviceService = new DeviceService(deviceMapper);
        DeviceRegisterResponse response = deviceService.register(request);

        // 6. 断言返回的 deviceId 为 dev_existing
        assertEquals("dev_existing", response.deviceId());

        // 7. 验证查询执行两次，插入尝试执行一次
        verify(deviceMapper, times(2)).findByFingerprint("install-race");
        verify(deviceMapper, times(1)).insert(any(Device.class));
    }

    /**
     * 撤销
     * 撤销不存在的设备，应该抛出携带 404 的异常。
     */
    @Test
    void revokingMissingDeviceReturnsNotFound() {
        // 1. 模拟 revoke("dev_missing") 返回 0
        when(deviceMapper.revoke("dev_missing")).thenReturn(0);

        DeviceService service = new DeviceService(deviceMapper);

        // 2. 使用 assertThrows 调用 service.revoke("dev_missing")
        //    并保存返回的异常
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.revoke("dev_missing")
        );

        // 3. 断言异常中的状态为 HttpStatus.NOT_FOUND
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        // 4. 验证 Mapper 的 revoke("dev_missing") 被调用一次.确认调用一次，且传入的是正确设备 ID
        verify(deviceMapper, times(1)).revoke("dev_missing");
    }

    /**
     * 撤销
     * 设备存在，撤销成功
     */
    @Test
    void revokingExistingDeviceSucceeds() {
        // 1. 模拟 revoke("dev_existing") 返回 1
        when(deviceMapper.revoke("dev_existing")).thenReturn(1);

        // 2. 创建 Service
        DeviceService deviceService = new DeviceService(deviceMapper);

        // 3. 用 assertDoesNotThrow 验证撤销方法正常结束
        assertDoesNotThrow(() -> deviceService.revoke("dev_existing"));

        // 4. 验证 Mapper 收到正确的设备 ID
        verify(deviceMapper).revoke("dev_existing");
    }

    /**
     * 测试分页查询设备列表接口：
     * 验证之前修复过的 空白系统筛选条件，防止以后改代码时再次出现“total 有值但列表为空”的问题。
     */
    @Test
    void blankOsIsTreatedAsNoFilter() {
        // 1. 模拟 countAllByOs(null) 返回 0L
        when(deviceMapper.countAllByOs(null)).thenReturn(0L);

        // 2. 模拟 findPageByOs(10, 0L, null) 返回空列表
        when(deviceMapper.findPageByOs(10, 0L, null)).thenReturn(List.of());    // 空列表不等于 null，而是List.of()

        DeviceService deviceService = new DeviceService(deviceMapper);

        // 3. 调用 list(1, 10, "   ")，保存响应
        DeviceListPageResponse response = deviceService.list(1, 10, "   ");

        // 4. 断言 total 为 0，items 为空
        assertEquals(0L, response.total());
        assertTrue(response.items().isEmpty());

        // 5. 验证两次 Mapper 调用收到的筛选值都是 null
        verify(deviceMapper).countAllByOs(null);
        verify(deviceMapper).findPageByOs(10, 0L, null);
    }

    /**
     * 测试分页查询列表接口
     * 把页码到偏移量的计算也纳入测试，同时验证普通筛选值会去掉首尾空格
     */
    @Test
    void listingDevicesCalculatesOffsetAndTrimsOs() {
        // 1. 模拟 countAllByOs("Android") 返回 12L
        when(deviceMapper.countAllByOs("Android")).thenReturn(12L);

        // 2. 模拟 findPageByOs(5, 10L, "Android") 返回空列表
        when(deviceMapper.findPageByOs(5, 10L, "Android")).thenReturn(List.of());

        DeviceService service = new DeviceService(deviceMapper);

        // 3. 调用 list(3, 5, "  Android  ")
        DeviceListPageResponse response = service.list(3, 5, "  Android   ");

        // 4. 检查响应 page=3、size=5、total=12
        assertEquals(3, response.page());
        assertEquals(5, response.size());
        assertEquals(12, response.total());

        // 5. 验证两条 Mapper 查询的参数
        verify(deviceMapper).countAllByOs("Android");
        verify(deviceMapper).findPageByOs(5, 10L, "Android");

    }

    /**
     * 测试分页查询列表接口
     * 验证非法分页参数在查询数据库之前被拒绝
     */
    @Test
    void invalidPageIsRejectedBeforeQueryingDatabase() {
        DeviceService service = new DeviceService(deviceMapper);

        // 1. 调用 list(0, 10, null)，断言抛出 ResponseStatusException
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.list(0, 10, null)
        );

        // 2. 检查异常状态为 BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        // 3. 验证 Mapper 没有被调用
        verifyNoInteractions(deviceMapper);
    }
}

