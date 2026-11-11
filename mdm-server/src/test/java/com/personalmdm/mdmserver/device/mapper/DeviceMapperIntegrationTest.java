package com.personalmdm.mdmserver.device.mapper;

import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.service.DeviceService;
import com.personalmdm.mdmserver.devicegroup.entity.DeviceGroup;
import com.personalmdm.mdmserver.devicegroup.mapper.DeviceGroupMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest             // 启动真实Spring应用上下文，默认不启动监听端口
@ActiveProfiles("test")     // 启动 application-test.properties 配置
public class DeviceMapperIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;  // 真正向MySQL执行查询

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceGroupMapper deviceGroupMapper;

    /**
     * 提取测试数据的创建方法
     * @return
     */
    private Device createDevice(String name, String os){
        Device device = new Device();
        device.setDeviceId("dev_" + UUID.randomUUID());
        device.setFingerprint("test_" + UUID.randomUUID());
        device.setDeviceName(name);
        device.setModel("simulator-v1");
        device.setOs(os);
        device.setOsVersion("15");
        device.setStatus(DeviceStatus.ACTIVE);
        return device;
    }

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

        Device first = createDevice("第一台测试设备", "Android");

        deviceMapper.insert(first);

        Device second = createDevice("第二台测试设备", "Android");
        // 覆盖辅助方法生成的指纹，构造重复条件
        second.setFingerprint(first.getFingerprint());

        assertThrows(
                DuplicateKeyException.class,
                () -> deviceMapper.insert(second)
        );
    }

    /**
     * 验证之前改名逻辑依赖的一个关键约定：名称没有变化时，Mapper 仍返回匹配行数 1。
     */
    @Test
    @Transactional
    void renamingToSameNameReturnsOneMatchedRow() {
        Device device = new Device();
        device.setDeviceId("dev_" + UUID.randomUUID());
        device.setFingerprint("test-" + UUID.randomUUID());
        device.setDeviceName("原名称");
        device.setModel("simulator-v1");
        device.setOs("Android");
        device.setOsVersion("15");
        device.setStatus(DeviceStatus.ACTIVE);

        deviceMapper.insert(device);

        int rows = deviceMapper.updateName(
                "原名称",
                device.getDeviceId()
        );

        // 核心验证
        assertEquals(1, rows);

        // 补充检查：更新后重新查询数据库，检查记录仍然存在，名称仍是预期值
        Device found = deviceMapper.findByDeviceId(device.getDeviceId());

        assertNotNull(found);
        assertEquals("原名称", found.getDeviceName());
    }

    /**
     * 验证名称确实能从旧值改成新值
     *
     * - 两台设备都已插入数据库。
     * - 只更新第一台。
     * - 重新查询两台，确认第一台名称改变，第二台名称未变。
     * - 第一台状态保持 ACTIVE。
     */
    @Test
    @Transactional
    void renamingDevicePersistsNewName() {
        Device device = new Device();
        device.setDeviceId("dev_" + UUID.randomUUID());
        device.setFingerprint("test-" + UUID.randomUUID());
        device.setDeviceName("旧名称");
        device.setModel("simulator-v1");
        device.setOs("Android");
        device.setOsVersion("15");
        device.setStatus(DeviceStatus.ACTIVE);

        deviceMapper.insert(device);

        Device another = new Device();

        another.setDeviceId("dev_" + UUID.randomUUID());
        another.setFingerprint("test-" + UUID.randomUUID());
        another.setDeviceName("另一个设备");
        another.setModel("simulator-v1");
        another.setOs("Android");
        another.setOsVersion("15");
        another.setStatus(DeviceStatus.ACTIVE);

        deviceMapper.insert(another);

        // 1. 调用 updateName，将这台设备改为“新名称”
        //    保存返回的行数
        int rows = deviceMapper.updateName("新名称", device.getDeviceId());

        // 2. 断言行数为 1
        assertEquals(1, rows);

        // 3. 根据 deviceId 重新查询设备，必须有这一步，否则断言直接查刚才手动赋值的设备对象，查的就是内存了
        Device found = deviceMapper.findByDeviceId(device.getDeviceId());
        Device anotherFound = deviceMapper.findByFingerprint(another.getFingerprint());

        // 4. 断言查询结果不为 null
        assertNotNull(found);
        assertNotNull(anotherFound);
        // 5. 断言查询到的名称为“新名称”
        assertEquals("新名称", found.getDeviceName());
        assertEquals("另一个设备", anotherFound.getDeviceName());
        // 6. 断言状态仍为 ACTIVE，避免误改其他字段
        assertEquals(DeviceStatus.ACTIVE, found.getStatus());
    }

    /**
     * 验证动态 SQL 的筛选与分页
     * 先按条件筛选，再排序，最后取指定页的数据。
     */
    @Test
    @Transactional
    void filteringAndPaginationReturnMatchingDevices() {
        // 1. 按上面的顺序插入三台设备
        Device first = createDevice("设备1", "Android");
        Device second = createDevice("设备2", "iOS");
        Device third = createDevice("设备3", "Android");

        deviceMapper.insert(first);
        deviceMapper.insert(second);
        deviceMapper.insert(third);

        // 2. 查询 Android 设备总数，预期为 2
        long total = deviceMapper.countAllByOsAndStatus("Android", null);
        assertEquals(2L, total);

        // 3. 查询 Android 第一页，每页 1 条，offset 为 0
        //    预期只有“Android新设备”
        List<Device> thirdDevice = deviceMapper.findPageByOsAndStatus(1, 0L, "Android", null);
        assertEquals(1,thirdDevice.size());
        assertEquals("设备3", thirdDevice.getFirst().getDeviceName());

        // 4. 查询 Android 第二页，每页 1 条，offset 为 1
        //    预期只有“Android旧设备”
        List<Device> firstDevice = deviceMapper.findPageByOsAndStatus(1, 1L, "Android", null);
        assertEquals(1,firstDevice.size());
        assertEquals("设备1", firstDevice.getFirst().getDeviceName());

    }

    /**
     * 验证设备心跳是否真的写入数据库
     */
    @Test
    @Transactional
    void activeDeviceHeartbeatIsRecorded() {
        Device device = createDevice("心跳测试设备", "Android");
        deviceMapper.insert(device);

        // 心跳前
        Device before = deviceMapper.findByDeviceId(device.getDeviceId());
        assertNotNull(before);
        assertNull(before.getLastHeartbeatTime());

        int rows = deviceMapper.updateHeartbeat(device.getDeviceId());

        assertEquals(1, rows);

        // 心跳后
        Device after = deviceMapper.findByDeviceId(device.getDeviceId());
        assertNotNull(after);
        assertNotNull(after.getLastHeartbeatTime());
    }

    /**
     * 已撤销设备的心跳不被记录
     */
    @Test
    @Transactional
    void revokedDeviceHeartbeatIsNotRecorded() {
        // 1. 创建测试设备，将状态设为 REVOKED，再插入
        Device device = createDevice("撤销的心跳设备", "Android");
        device.setStatus(DeviceStatus.REVOKED);
        deviceMapper.insert(device);

        // 2. 调用 updateHeartbeat，预期返回 0
        int row = deviceMapper.updateHeartbeat(device.getDeviceId());
        assertEquals(0, row);

        // 3. 重新查询，确认心跳时间仍为 null
        Device after = deviceMapper.findByDeviceId(device.getDeviceId());
        assertNotNull(after);
        assertNull(after.getLastHeartbeatTime());
    }

    /**
     * 分页查询设备列表
     * 系统和状态两个条件都同时满足
     */
    @Test
    @Transactional
    void  filteringByOsAndStatusReturnOnlyMatchingDevices(){
        // 创建三台设备
        Device androidActive = createDevice("androidActive", "Android");

        Device androidRevoked= createDevice("androidRevoked", "Android");
        androidRevoked.setStatus(DeviceStatus.REVOKED);

        Device iOSRevoked = createDevice("iOSRevoked", "iOS");
        iOSRevoked.setStatus(DeviceStatus.REVOKED);

        // 存入数据库
        deviceMapper.insert(androidActive);
        deviceMapper.insert(androidRevoked);
        deviceMapper.insert(iOSRevoked);

        // 1. 调用 countAllByOsAndStatus
        //    筛选 Android + REVOKED,断言总数为 1L
        long row = deviceMapper.countAllByOsAndStatus("Android", DeviceStatus.REVOKED);
        assertEquals(1L, row);
        //    筛选 null + REVOKED，断言总数为2L
        long row1 = deviceMapper.countAllByOsAndStatus(null, DeviceStatus.REVOKED);
        assertEquals(2L, row1);


        // 2. 调用 findPageByOsAndStatus
        //    size=10、offset=0L，使用同样的筛选条件,断言列表只有一条记录，这条记录的 deviceId 等于 revokedAndroid 的 deviceId
        List<Device> deviceList= deviceMapper.findPageByOsAndStatus(10, 0L, "Android", DeviceStatus.REVOKED);
        assertEquals(1, deviceList.size());
        assertEquals(androidRevoked.getDeviceId(), deviceList.getFirst().getDeviceId());

        //    size=10、offset=0L，使用同样的筛选条件,断言列表有2条记录，两条记录的状态都是撤销状态
        List<Device> deviceList1= deviceMapper.findPageByOsAndStatus(10, 0L, null, DeviceStatus.REVOKED);
        assertEquals(2, deviceList1.size());
        assertTrue( // 断言结果必须为true
                // 让列表中的设备参与逐个检查
                deviceList1.stream()
                        /**
                         * allMatch检查集合中的每个元素，是否都满足指定条件.只有全部满足才会全部返回true
                         * 边界：
                            * 如果查询出问题返回的是空列表，allmatch仍然会返回true
                            * 因为列表中没有违反条件的设备，所以前一个判断列表数量（是否有预期的设备）的断言非常重要
                         */
                        .allMatch(
                                // 描述检查规则
                                device  // 代表当前正在检查的一台设备
                                ->
                                device.getStatus() == DeviceStatus.REVOKED  // 判断这台设备的状态是否是REVOKED
                        )
        );
    }

    /**
     * 测试：
     * 激活状态设备组移动成功，设备组更新语句返回行数1
     * 撤销状态设备组移动失败，设备组更新语句返回行数0
     */
    @Test
    @Transactional
    void testUpdateDeviceGroupFunctionAvailable() {
        Device active = createDevice("ActiveDevice", "Android");
        Device revoked = createDevice("RevockedDevice", "Android");
        revoked.setStatus(DeviceStatus.REVOKED);

        DeviceGroup A = new DeviceGroup();
        A.setGroupName("组A");
        deviceGroupMapper.insert(A);
        active.setGroupId(A.getId());
        revoked.setGroupId(A.getId());

        // 在设置设备组后，再将设备插入数据库
        deviceMapper.insert(active);
        deviceMapper.insert(revoked);

        DeviceGroup B = new DeviceGroup();
        B.setGroupName("组B");
        deviceGroupMapper.insert(B);

        // 确认在调用更新设备组方法之前两台设备都在组A
        assertEquals(
                A.getId(),  // 期望值
                deviceMapper.findByDeviceId(active.getDeviceId())   // 同时检验设备是否成功存入数据库，并取出设备的组id
                        .getGroupId()
        );
        assertEquals(
                A.getId(),  // 期望值
                deviceMapper.findByDeviceId(revoked.getDeviceId())   // 同时检验设备是否成功存入数据库，并取出设备的组id
                        .getGroupId()
        );


        // 执行更新设备组方法
        int activeRows = deviceMapper.updateDeviceGroup(B.getId(), active.getDeviceId());
        int revockedRows = deviceMapper.updateDeviceGroup(B.getId(), revoked.getDeviceId());

        // 检验更新结果
        assertEquals(1,activeRows);
        assertEquals(0, revockedRows);

        // 确认激活设备移到了B，未激活设备还在原地
        assertEquals(
                B.getId(),  // 期望值
                deviceMapper.findByDeviceId(active.getDeviceId())   // 同时检验设备是否成功存入数据库，并取出设备的组id
                        .getGroupId()
        );
        assertEquals(
                A.getId(),  // 期望值
                deviceMapper.findByDeviceId(revoked.getDeviceId())   // 同时检验设备是否成功存入数据库，并取出设备的组id
                        .getGroupId()
        );
    }
}
