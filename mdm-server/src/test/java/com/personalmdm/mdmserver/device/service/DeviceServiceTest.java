// 测试类位置
package com.personalmdm.mdmserver.device.service;

// 测试所需的类
import com.personalmdm.mdmserver.device.dto.*;
import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.mapper.DeviceMapper;
import com.personalmdm.mdmserver.device.mapper.DeviceStatusHistoryMapper;
import com.personalmdm.mdmserver.devicegroup.entity.DeviceGroup;
import com.personalmdm.mdmserver.devicegroup.mapper.DeviceGroupMapper;
import com.personalmdm.mdmserver.devicegroup.service.DeviceGroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// 静态导入：允许直接使用方法，不用每次都写完整的类名
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    @Mock
    private DeviceGroupService deviceGroupService;
    @Mock
    private DeviceStatusHistoryMapper deviceStatusHistoryMapper;

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
        existing.setStatus(DeviceStatus.REVOKED);    // 设备已撤销

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
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

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
        existing.setStatus(DeviceStatus.REVOKED);

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
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

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
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

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
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

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
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

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
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );
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

        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

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
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

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
        when(deviceMapper.countAllByOsAndStatus(null,null)).thenReturn(0L);

        // 2. 模拟 findPageByOs(10, 0L, null) 返回空列表
        when(deviceMapper.findPageByOsAndStatus(10, 0L, null,null)).thenReturn(List.of());    // 空列表不等于 null，而是List.of()

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // 3. 调用 list(1, 10, "   ")，保存响应
        DeviceListPageResponse response = deviceService.list(1, 10, "   ",null);

        // 4. 断言 total 为 0，items 为空
        assertEquals(0L, response.total());
        assertTrue(response.items().isEmpty());

        // 5. 验证两次 Mapper 调用收到的筛选值都是 null
        verify(deviceMapper).countAllByOsAndStatus(null,null);
        verify(deviceMapper).findPageByOsAndStatus(10, 0L, null,null);
    }

    /**
     * 测试分页查询列表接口
     * 把页码到偏移量的计算也纳入测试，同时验证普通筛选值会去掉首尾空格
     */
    @Test
    void listingDevicesCalculatesOffsetAndTrimsOs() {
        // 1. 模拟 countAllByOs("Android") 返回 12L
        when(deviceMapper.countAllByOsAndStatus("Android",null)).thenReturn(12L);

        // 2. 模拟 findPageByOs(5, 10L, "Android") 返回空列表
        when(deviceMapper.findPageByOsAndStatus(5, 10L, "Android",null)).thenReturn(List.of());

        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // 3. 调用 list(3, 5, "  Android  ")
        DeviceListPageResponse response = service.list(3, 5, "  Android   ",null);

        // 4. 检查响应 page=3、size=5、total=12
        assertEquals(3, response.page());
        assertEquals(5, response.size());
        assertEquals(12, response.total());

        // 5. 验证两条 Mapper 查询的参数
        verify(deviceMapper).countAllByOsAndStatus("Android",null);
        verify(deviceMapper).findPageByOsAndStatus(5, 10L, "Android",null);

    }

    /**
     * 测试分页查询列表接口
     * 验证非法分页参数在查询数据库之前被拒绝
     */
    @Test
    void invalidPageIsRejectedBeforeQueryingDatabase() {
        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // 1. 调用 list(0, 10, null)，断言抛出 ResponseStatusException
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.list(0, 10, null,null)
        );

        // 2. 检查异常状态为 BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        // 3. 验证 Mapper 没有被调用
        verifyNoInteractions(deviceMapper);
    }

    /**
     * 测试分页查询列表接口
     * 参数化测试：用同一段测试逻辑检查多组输入
     */
    @ParameterizedTest
    @CsvSource({
            "0, 10",
            "-1, 10",
            "1, 0",
            "1, -1",
            "1, 101"
    })
    void invalidPaginationIsRejectedBeforeQueryingDatabase(
            int page,
            int size
    ){
        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.list(page, size, null,null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(deviceMapper);
    }

    /**
     * 测试分页查询列表接口
     * 合法值边界测试
     */
    @Test
    void maximumPageSizeIsAccepted() {
        // 模拟 countAllByOs(null) 返回 0L
        when(deviceMapper.countAllByOsAndStatus(null,null)).thenReturn(0L);

        // 模拟 findPageByOs(100, 0L, null) 返回空列表
        when(deviceMapper.findPageByOsAndStatus(100, 0L, null,null)).thenReturn(List.of());

        // 调用 list(1, 100, null)
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );
        DeviceListPageResponse response = deviceService.list(1, 100, null,null);

        // 检查响应 size 为 100
        assertEquals(100, response.size());

        // 验证分页查询收到正确参数
        verify(deviceMapper).findPageByOsAndStatus(100, 0L, null,null);
    }

    /**
     * 测试设备心跳获取接口
     * verifyNoInteractions(mock) 检查整个模拟对象没有调用；verify(mock, never()).某方法(...) 检查特定方法没有调用。
     */
    @Test
    void successfulHeartbeatDoesNotQueryDeviceAgain() {
        // 1. 模拟 updateHeartbeat("dev_active") 返回 1
        when(deviceMapper.updateHeartbeat("dev_active")).thenReturn(1);

        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // 2. 执行心跳，确认正常结束：使用 assertDoesNotThrow 调用 heartbeat("dev_active")
        assertDoesNotThrow(() -> service.heartbeat("dev_active"));

        // 3. 确认更新调用一次，且设备ID正确：验证 updateHeartbeat 收到了正确的设备 ID 参数
        verify(deviceMapper).updateHeartbeat("dev_active");

        // 4. 验证没有调用 findByDeviceId
        verify(deviceMapper, never()).findByDeviceId("dev_active");

    }


    /**
     * 设备心跳接口：设备不存在时返回404
     */
    @Test
    void heartbeatForMissingDeviceReturnsNotFound() {
        // 1. 模拟 updateHeartbeat("dev_missing") 返回 0
        when(deviceMapper.updateHeartbeat("dev_missing")).thenReturn(0);

        // 2. 模拟 findByDeviceId("dev_missing") 返回 null
        when(deviceMapper.findByDeviceId("dev_missing")).thenReturn(null);

        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // 3. 使用 assertThrows 调用 heartbeat("dev_missing")
        //    保存 ResponseStatusException
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.heartbeat("dev_missing")
        );

        // 4. 检查异常状态为 NOT_FOUND
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        // 5. 验证更新和查询分别被调用一次，参数均为 dev_missing
        verify(deviceMapper, times(1)).updateHeartbeat("dev_missing");
        verify(deviceMapper, times(1)).findByDeviceId("dev_missing");
    }

    /**
     * 设备心跳获取接口：设备已撤销时返回403
     */
    @Test
    void heartbeatForRevokedDeviceReturnsForbidden() {
        Device existing = new Device();
        existing.setStatus(DeviceStatus.REVOKED);

        // 1. 模拟 updateHeartbeat("dev_revoked") 返回 0
        when(deviceMapper.updateHeartbeat("dev_revocked")).thenReturn(0);

        // 2. 模拟 findByDeviceId("dev_revoked") 返回 existing
        when(deviceMapper.findByDeviceId("dev_revocked")).thenReturn(existing);

        // 3. 创建 Service，调用 heartbeat，捕获异常
        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.heartbeat("dev_revocked")
        );

        // 4. 断言异常状态为 FORBIDDEN
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        // 5. 验证更新和查询各调用一次，参数正确
        verify(deviceMapper).findByDeviceId("dev_revocked");
        verify(deviceMapper).updateHeartbeat("dev_revocked");
    }

    /**
     * 设备心跳获取接口：更新失败但查询到激活设备时返回409
     */
    @Test
    void heartbeatReturnsConflictWhenUpdateFailsButDeviceIsActive() {
        // 准备一个模拟设备存在于数据库中
        Device device = new Device();
        device.setStatus(DeviceStatus.ACTIVE);

        // 模拟更新返回 0，随后查询到 ACTIVE 设备
        when(deviceMapper.updateHeartbeat("dev_conflict")).thenReturn(0);
        when(deviceMapper.findByDeviceId("dev_conflict")).thenReturn(device);

        // 调用 heartbeat，断言抛出携带 CONFLICT 的异常
        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );
        ResponseStatusException exception  = assertThrows(
                ResponseStatusException.class,
                () -> service.heartbeat("dev_conflict")
        );

        // 验证409
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        // 验证更新和查询各执行一次
        verify(deviceMapper).findByDeviceId("dev_conflict");
        verify(deviceMapper).updateHeartbeat("dev_conflict");
    }

    /**
     * 验证心跳时间确实传入计算器
     * 补一个 Service 层的衔接测试，确认心跳时间确实传给了计算器，计算结果也进入详情 DTO。
     *
     * 编译器检查类型，测试可以进一步检查具体值和业务行为。
     */
    @Test
    void deviceDetailIncludesCalculatedConnectionStatus() {
        // 准备数据
        LocalDateTime heartbeat = LocalDateTime.of(
                2026, 10, 27, 10, 0
        );

        Device device = new Device();
        device.setDeviceId("dev_existing");
        device.setStatus(DeviceStatus.ACTIVE);
        device.setLastHeartbeatTime(heartbeat);

        DeviceConnectionCalculator calculator = mock(DeviceConnectionCalculator.class);

        // 预设
        when(deviceMapper.findByDeviceId("dev_existing")).thenReturn(device);

        when(calculator.calculate(
                eq(heartbeat),              // 第一个参数必须等于指定的心跳时间。
                any(LocalDateTime.class)    // 第二个参数接受任意非空的当前时间
        )).thenReturn(DeviceConnectionStatus.ONLINE);

        // 执行
        DeviceService service = new DeviceService(
                deviceMapper,
                calculator,
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        DeviceDetailResponse response = service.getDetail("dev_existing");

        // 验证
        assertEquals(heartbeat, response.lastHeartbeatTime());
        assertEquals("ONLINE", response.connectionStatus());

        verify(calculator).calculate(
                eq(heartbeat),
                any(LocalDateTime.class)
        );

    }

    /**
     * 设备在线状态验证
     * 用固定时钟和真实计算器，验证详情接口在恰好两分钟时返回 ONLINE。
     */
    @Test
    void detailIsOnlineExactlyTwoMinutesAfterHeartbeat() {
        // 准备-固定时钟：UTC时间02：02+上海时区=当地时间10：02
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-27T02:02:00Z"),  // 表示 UTC 时间 02:02
                ZoneId.of("Asia/Shanghai")            // 表示结合上海时区
        );

        // 准备-设备
        Device device = new Device();
        device.setDeviceId("dev_existing");
        device.setStatus(DeviceStatus.ACTIVE);
        device.setLastHeartbeatTime(
                LocalDateTime.of(2026, 10, 27, 10, 0)
        );

        // 预设-查询设备id返回device
        when(deviceMapper.findByDeviceId("dev_existing")).thenReturn(device);

        // 初始化服务层
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                clock,
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        DeviceDetailResponse response = deviceService.getDetail("dev_existing");

        // 判断-设备在线
        assertEquals("ONLINE", response.connectionStatus());
        // 判断-设备最后一次心跳时间正确
        assertEquals(
                device.getLastHeartbeatTime(),
                response.lastHeartbeatTime()
        );

    }

    /**
     * 设备离线状态验证
     */
    @Test
    void detailIsOfflineTwoMinutesOneNanoAfterHeartbeat() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-27T02:02:01Z"),
                ZoneId.of("Asia/Shanghai")
        );

        // 准备-设备
        Device device = new Device();
        device.setDeviceId("dev_existing");
        device.setStatus(DeviceStatus.ACTIVE);
        device.setLastHeartbeatTime(
                LocalDateTime.of(
                        2026, 10, 27, 10, 0
                )
        );

        // 预设
        when(deviceMapper.findByDeviceId("dev_existing")).thenReturn(device);

        // 初始化服务器
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                clock,
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        DeviceDetailResponse response = deviceService.getDetail("dev_existing");

        assertEquals("OFFLINE", response.connectionStatus());
        assertEquals(device.getLastHeartbeatTime(), response.lastHeartbeatTime());
    }

    /**
     * 检验设备注册：设备插入失败情况下的处理
     */
    @Test
    void newDeviceRegistersInsertFail() {
        // 准备数据
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-27T02:02:01Z"),
                ZoneId.of("Asia/Shanghai")
        );
        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setFingerprint("dev_new_fingerprint");

        when(deviceMapper.findByFingerprint("dev_new_fingerprint")).thenReturn(null);
        when(deviceMapper.insert(any(Device.class))).thenReturn(0);

        DeviceService service = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                clock,
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // `assertThrows` 已经会检查异常是否抛出
        assertThrows(
                IllegalStateException.class,
                () -> service.register(request)
        );

        verify(deviceMapper).findByFingerprint("dev_new_fingerprint");
        verify(deviceMapper).insert(any(Device.class));
    }

    /**
     * 测试设备组更新方法
     * 前提：
         * 第一次查询设备，返回 ACTIVE，当前在组 A。
         * 目标组 B 存在。
         * 条件更新返回 0。
         * 第二次查询设备，返回 REVOKED。
     */
    @Test
    void removeDeviceGroupAtFirstDeviceIsActiveButUpdateReturnZeroRecheckDeviceIsReVoked() {
        String deviceId = "dev_test";
        Long groupId = 1L;

        Device active = new Device();
        active.setStatus(DeviceStatus.ACTIVE);

        Device revoked = new Device();
        revoked.setStatus(DeviceStatus.REVOKED);

        // 第一次查询 激活设备， 第二次查询 撤销设备
        when(deviceMapper.findByDeviceId(deviceId)).thenReturn(active, revoked);

        // 更新设备组返回0
        when(deviceMapper.updateDeviceGroup(groupId, deviceId)).thenReturn(0);

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> deviceService.moveToGroup(deviceId, groupId)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(deviceMapper, times(2)).findByDeviceId(deviceId);
        verify(deviceMapper).updateDeviceGroup(groupId, deviceId);

    }

    /**
     * 设备组更新
     * 测试场景：更新返回0，但查重发现设备已经在目标组，正常结束
     * 注意：
        * `deviceGroupService` 是 @Mock。 它的 requireExists() 默认正常结束，不会执行真实方法，也就不会调用 deviceGroupMapper。
     */
    @Test
    void moveToGroupSucceedsWhenUpdateReturnsZeroAndAlreadyInTargetGroup() {
        String deviceId = "dev_test";
        Long groupId = 1000L;

        Device device = new Device();
        device.setStatus(DeviceStatus.ACTIVE);

//        DeviceGroup group = new DeviceGroup();
//        group.setId(groupId);

        Device latest = new Device();
        latest.setStatus(DeviceStatus.ACTIVE);
//        latest.setGroupId(groupId); 复用同一个Long对象引用，指向同一个对象，但是我们的设计目标是两个不同的Long对象，值相同，所以此处要改为独立取得同值的Long
        latest.setGroupId(Long.valueOf("1000"));



        // 更新返回0
        when(deviceMapper.updateDeviceGroup(groupId, deviceId)).thenReturn(0);
        when(deviceMapper.findByDeviceId(deviceId)).thenReturn(device, latest);
//        when(deviceGroupMapper.findById(groupId)).thenReturn(group);

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        assertDoesNotThrow(
                () -> deviceService.moveToGroup(deviceId, groupId)
        );


        verify(deviceMapper, times(2)).findByDeviceId(deviceId);
        verify(deviceMapper).updateDeviceGroup(groupId, deviceId);
//        verify(deviceGroupMapper).findById(groupId);
        // 验证service的协作
        verify(deviceGroupService).requireExists(groupId);
    }

    /**
     * 设备组更新
     * 测试场景：第一次查询就返回“已撤销“状态，预期抛出403
     */
    @Test
    void moveToGroupFailedBecauseRevokedStatusWhenFirstCheck() {
        Device revoked = new Device();
        revoked.setStatus(DeviceStatus.REVOKED);

        when(deviceMapper.findByDeviceId("dev_revoked")).thenReturn(revoked);

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> deviceService.moveToGroup("dev_revoked", Long.valueOf("1000"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        // 验证mapper的执行。当第一次就返回已撤销时，本次moveTpGroup调用只会执行一次设备业务ID的查询，剩下的查询不会做，不会做的部分也要验证
        verify(deviceMapper).findByDeviceId("dev_revoked");

        // 改成 any() 后，可以验证任何参数的分组更新都没有发生。
        verify(deviceMapper, never()).updateDeviceGroup(any(), any());
        verifyNoInteractions(deviceGroupService);
    }

    /**
     * 设备组更新
     * 测试场景：目标组不存在
     */
    @Test
    void moveToGroupFailedBecauseTargetGroupIsNotFound() {
        Device device = new Device();
        device.setStatus(DeviceStatus.ACTIVE);

        when(deviceMapper.findByDeviceId("dev_active")).thenReturn(device);

        doThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "目标设备组不存在！")
        ).when(deviceGroupService).requireExists(Long.valueOf("1000"));

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> deviceService.moveToGroup("dev_active", Long.valueOf("1000"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(deviceMapper).findByDeviceId("dev_active");
        verify(deviceMapper, never()).updateDeviceGroup(any(), any());
        verify(deviceGroupService).requireExists(Long.valueOf("1000"));
    }


    /**
     * 设备组更新
     * 测试场景：正常更新
     */
    @Test
    void moveToGroupSucceed() {
        String deviceId = "dev_active";
        Long groupId = 1000L;
        Device device = new Device();
        device.setStatus(DeviceStatus.ACTIVE);

        when(deviceMapper.findByDeviceId(deviceId)).thenReturn(device);
//        只返回 1，不会执行 SQL，也不会给 device 调用 setGroupId()
        when(deviceMapper.updateDeviceGroup(groupId, deviceId)).thenReturn(1);

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // 成功执行没有抛出异常
        assertDoesNotThrow(
                () -> deviceService.moveToGroup(deviceId, groupId)
        );

       verify(deviceMapper).findByDeviceId(deviceId);
       verify(deviceMapper).updateDeviceGroup(groupId, deviceId);
       verify(deviceGroupService).requireExists(groupId);
    }

    /**
     * 设备组更新
     * 测试情况：更新返回0，重查设备是激活状态但是组id是2000L
     */
    @Test
    void moveToGroupFailedButCannotFindTheReason() {
        Device device = new Device();
        device.setStatus(DeviceStatus.ACTIVE);
        device.setGroupId(Long.valueOf("2000"));

        Long targetGroup = 1000L;
        String deviceId = "dev_exist";

        when(deviceMapper.findByDeviceId(deviceId)).thenReturn(device, device);
        when(deviceMapper.updateDeviceGroup(targetGroup, deviceId)).thenReturn(0);

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // assertThrows断言有IllegalStateException.class异常抛出
        assertThrows(
                IllegalStateException.class,
                () -> deviceService.moveToGroup(deviceId, targetGroup)
        );

        verify(deviceMapper, times(2)).findByDeviceId(deviceId);
        verify(deviceMapper).updateDeviceGroup(targetGroup, deviceId);
        verify(deviceGroupService).requireExists(targetGroup);
    }

    /**
     * 设备组更新
     * 测试场景：更新设备组语句执行的前一刻设备组被删除了，返回404
     */
    @Test
    void moveToGroupReturns404WhenTargetGroupDisappearsDuringUpdate() {
        String deviceId = "dev_exist";
        Long groupId = 1000L;
        // 更新前检查设备组存在
        Device device = new Device();
        device.setStatus(DeviceStatus.ACTIVE);

        // 设备存在且激活预设，保证更新前检查可通过
        when(deviceMapper.findByDeviceId(deviceId)).thenReturn(device);
        // 更新方法抛出异常
        when(deviceMapper.updateDeviceGroup(groupId, deviceId))
                .thenThrow(
                        new DataIntegrityViolationException("外键约束失败!")
                );
        // 检查设备组存在性: 预设结果前置写法，第一次通过，第二次抛出异常
        doNothing()
                .doThrow(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "目标设备组不存在！")
                )
                .when(deviceGroupService).requireExists(groupId);

        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> deviceService.moveToGroup(deviceId, groupId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(deviceMapper).findByDeviceId(deviceId);
        verify(deviceGroupService, times(2)).requireExists(groupId);
        verify(deviceMapper).updateDeviceGroup(groupId, deviceId);
    }

    /**
     * 设备组更新
     * 测试场景：更新抛出异常，但目标组仍然存在，继续抛出原异常
     */
    @Test
    void moveToGroupRethrowsIntegrityViolationWhenTargetGroupStillExists() {
        // 设备存在且激活
        Device device = new Device();
        device.setStatus(DeviceStatus.ACTIVE);

        // 设备ID
        String deviceId = "dev_test";
        // 设备组ID
        Long groupId = 1000L;

        // 保存异常对象
        DataIntegrityViolationException original = new DataIntegrityViolationException("数据完整性约束失败！");

        // 预设：更新前检查设备存在性与状态
        when(deviceMapper.findByDeviceId(deviceId)).thenReturn(device);

        // 预设：设备组存在性检查，都要通过，mock默认自动通过

        // 预设：执行设备组更新时抛出更新异常
        when(deviceMapper.updateDeviceGroup(groupId, deviceId)).thenThrow(original);

        // 构造service
        DeviceService deviceService = new DeviceService(
                deviceMapper,
                new DeviceConnectionCalculator(),
                Clock.systemDefaultZone(),
                deviceGroupService,
                deviceStatusHistoryMapper
        );

        // 执行
        DataIntegrityViolationException actual = assertThrows(
                DataIntegrityViolationException.class,
                () -> deviceService.moveToGroup(deviceId, groupId)
        );

        assertSame(original, actual);
        verify(deviceMapper).findByDeviceId(deviceId);
        verify(deviceGroupService, times(2)).requireExists(groupId);
        verify(deviceMapper).updateDeviceGroup(groupId, deviceId);
    }
}

