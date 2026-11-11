package com.personalmdm.mdmserver.device.service;

import com.personalmdm.mdmserver.device.dto.*;
import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.mapper.DeviceMapper;
import com.personalmdm.mdmserver.devicegroup.service.DeviceGroupService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @Service：告诉 Spring 这是一个业务服务组件
 */
@Service
public class DeviceService {
    /**
     * 声明依赖
     * why private final？
     * ans：DeviceService 正常运行时就应该依赖一个确定的 Mapper。使用 final 能明确表达这个依赖是必需的，不应该在对象创建后随意更换。
     */
    private final DeviceMapper deviceMapper;
    private final DeviceConnectionCalculator deviceConnectionCalculator;
    private Clock clock;    // 注入 Java 自带的 Clock，让应用使用真实时间，测试使用固定时间
    private final DeviceGroupService deviceGroupService;
    /**
     * 构造方法
     * 构造器注入：Spring 创建 DeviceService 时，把 MyBatis 提供的 DeviceMapper 对象传进来。只有一个构造方法时，无须写 @Autowired。
     * 当 Spring 扫描到这个 Service，并且容器中存在可注入的 DeviceMapper Bean 时，Spring 就可以调用这个构造器，把 Mapper 传进来。
     * @param deviceMapper
     * @param deviceConnectionCalculator 设备在线状态计算器
     */
    public DeviceService(
            DeviceMapper deviceMapper,
            DeviceConnectionCalculator deviceConnectionCalculator,
            Clock clock,
            DeviceGroupService deviceGroupService
    ){
        this.deviceMapper = deviceMapper;
        this.deviceConnectionCalculator = deviceConnectionCalculator;
        this.clock = clock;
        this.deviceGroupService = deviceGroupService;
    }


    /**
     * 设备注册方法
     * 注册流程：查询 → 判断是否存在 → 创建对象 → 保存 → 返回身份
     * @param request 设备注册请求DTO（即设备发送给服务器的内容）
     * @return        设备注册响应 DTO（即服务器返回给设备的内容）
     */
    public DeviceRegisterResponse register(DeviceRegisterRequest request){

        // 查询设备是否已注册成功：查询设备指纹是否已经存在
        Device existing =  deviceMapper.findByFingerprint(request.getFingerprint());

        // ===== （幂等性）如果设备指纹存在于数据库中，则返回数据库中的device_id =====
        if(existing != null){
            /**
             * 1. 创建一个响应 DTO 对象，把这个设备标识传给它的构造器
             * 2. 将响应对象返回给调用方，同时结束当前 register() 方法的执行
             */
            return buildRegisterResponse(existing);
        }

        // ===== 如果设备指纹并不存在于数据库中，则实现新设备注册 =====

        // 创建一个新的Device对象
        Device device = new Device();

        // 生成设备业务标识
        device.setDeviceId("dev_"+ UUID.randomUUID());

        // 将请求中的五个字段复制到 Device
        device.setDeviceName(request.getDeviceName());
        device.setFingerprint(request.getFingerprint());
        device.setModel(request.getModel());
        device.setOs(request.getOs());
        device.setOsVersion(request.getOsVersion());

        // 设备状态设置为激活
        device.setStatus(DeviceStatus.ACTIVE);

        /**
         * 设备注册的并发异常处理
         * 1. 正常插入成功，返回自己生成的deviceId
         * 2. 遇到唯一键冲突(DuplicateKeyException)，再查询同一个 fingerprint
         * 3. 查询记录，说明已有对应注册身份，返回它
         * 4. 查不到就重新抛出原异常
         */
        int rows;
        try {
            // 将设备插入数据库，插入本身就是原子操作
            rows = deviceMapper.insert(device);
        }catch (DuplicateKeyException e){   // 捕获唯一键冲突
            // 查询同一个 fingerprint
            Device registered = deviceMapper.findByFingerprint(request.getFingerprint());

            // 判断是否已有注册身份
            if(registered != null){
                // 已有注册身份，返回对应的设备ID
                return buildRegisterResponse(registered);
            }

            /**
             * 没有注册身份，重新抛出原异常
             * quest:为什么捕获异常后不能直接认定是重复设备？
             * ans: 因为表里还有 device_id 的唯一约束，DuplicateKeyException 本身并不保证冲突来自 fingerprint。
             */
            throw e;
        }

        if (rows != 1) {
            throw new IllegalStateException("设备注册失败，预期插入 1 行，实际插入 " + rows + " 行。");
        }
        // 注册成功，返回设备ID
        return new DeviceRegisterResponse(device.getDeviceId());
    }

    /**
     * 构建设备注册响应 DTO
     * 判断设备是否处于撤销状态，处于撤销状态的设备不允许重新注册或找回设备ID 即抛出 403 forbidden的响应
     * @param device    设备实体
     * @return          设备注册响应 DTO
     */
    private DeviceRegisterResponse buildRegisterResponse(Device device){
        if(device.getStatus() == DeviceStatus.REVOKED){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "设备已撤销，不允许重新注册！");
        }

        return new DeviceRegisterResponse(device.getDeviceId());
    }

    /**
     * 修改设备名称
     * @param deviceId 目标设备ID
     * @param request  目标设备名称
     */
    public void rename(String deviceId, DeviceRenameRequest request){
        /**
         * update操作本身就具有原子性；device_id具有唯一性约束
         * 因此，使用条件update可以直接避免“先查后更新”两个操作的间隔期间可能发生的设备删除情况
         * 同时也减少了一次数据库操作
         */
        // 如果设备存在，且名称改变，则更新设备名称
        int row = deviceMapper.updateName(request.getDeviceName(), deviceId);

        // 应对“查询后被删除”的情况，返回404
        if(row==0){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在");
        }

    }

    /**
     * 返回设备详情
     * @param deviceId 设备ID
     * @return         设备详情响应 DTO
     */
    public DeviceDetailResponse getDetail(String deviceId){
        // 使用deviceId查询设备
        Device device = deviceMapper.findByDeviceId(deviceId);

        // 如果设备不存在，则返回404
        if(device == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在");
        }

        // 如果设备存在，则返回设备详情
        DeviceConnectionStatus connectionStatus = deviceConnectionCalculator.calculate(
                device.getLastHeartbeatTime(),
                LocalDateTime.now(clock)
        );
        return new DeviceDetailResponse(
                device.getDeviceId(),
                device.getDeviceName(),
                device.getModel(),
                device.getOs(),
                device.getOsVersion(),
                device.getStatus().name(),
                device.getCreateTime(),
                device.getLastHeartbeatTime(),
                connectionStatus.name(),
                device.getGroupId()
        );
    }

    /**
     * 返回设备列表
     * @param page  页码
     * @param size  单词实际返回的条数
     * @return      设备列表页面响应 DTO
     */
    public DeviceListPageResponse list(int page, int size, String os, DeviceStatus status){
        // 参数合法性校验
        if(page < 1 || size < 1 || size > 100){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page 必须至少为 1，size 必须在 1 到 100 之间"
            );
        }

        // 偏移量计算
        long offset = (page - 1L) * size;

        /**
         * 将空字符串和空格统一转换为null，表示不筛选
         *
         * 三元运算符语法：条件 ? 条件成立时的值 : 条件不成立时的值
         * os.strip()：去除首尾空白后的字符串
         * isEmpty() 通常用于判断字符串是否为空字符串 ""。但它不能判断 null，也不能判断字符串是否只包含空格
         * Java 的 String 变量可以重新赋值，但是对象中的字符内容不能被修改
         * 代码使用 os和normalizedOs两个变量，是为了保留原始输入os，给 normalizedOs 重新赋值，不会影响 os。只有变量声明为 final 时，才不能再次给这个变量赋值
         */
        String normalizedOs = os == null ? null : os.strip();
        if (normalizedOs != null && normalizedOs.isEmpty()){
            normalizedOs = null;
        }

        // 注意：分页总数和列表必须使用相同的筛选条件

        // 设备总数
        long total = deviceMapper.countAllByOsAndStatus(normalizedOs,status);

        // 设备列表获取
        List<Device> deviceList = deviceMapper.findPageByOsAndStatus(size, offset, normalizedOs, status);

        // 设备列表包装成 设备列表条目响应 DTO 列表
        List<DeviceListItemResponse> items = new ArrayList<>();
        for(Device device : deviceList){
            DeviceListItemResponse item = new DeviceListItemResponse(
                    device.getDeviceId(),
                    device.getDeviceName(),
                    device.getModel(),
                    device.getOs(),
                    device.getStatus().name()
            );
            items.add(item);
        }

        return new DeviceListPageResponse(page, size, total, items);
    }

    /**
     * 撤销设备
     * 状态转换：激活->撤销
     * @param deviceId 设备ID
     */
    public void revoke(String deviceId){

        // 根据更新语句返回的匹配结果判定是否更新成功
        int row = deviceMapper.revoke(deviceId);

        // 设备不存在时，更新语句匹配结果为0
        if(row == 0){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在");
        }
    }

    /**
     * 获取设备心跳
     * @param deviceId 设备业务ID
     */
    public void heartbeat(String deviceId){

        int rows = deviceMapper.updateHeartbeat(deviceId);

        // 心跳更新成功
        if (rows == 1){
            return;
        }

        // 心跳更新失败，撤销设备 or 设备不存在
        Device existing = deviceMapper.findByDeviceId(deviceId);

        // 404: 设备不存在
        if (existing == null){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "设备不存在"
            );
        }

        // 设备存在，但已被撤销
        if (existing.getStatus() == DeviceStatus.REVOKED){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "设备已撤销，不能上报心跳"
            );
        }

        /**
         * 409 Conflict兜底
         * 更新没有成功，但后续查询也无法把原因归结为“不存在”或“已撤销”
         * 例如：
         * 1. 心跳请求 A：执行 UPDATE，此时设备是 REVOKED，返回 0
         * 2. 管理员操作 B：吧设备恢复成 ACTIVE
         * 3. 心跳请求 A：在排查更新失败原因的时候，发现它存在，而且是 ACTIVE
         */
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "设备状态已变化，请重试"
        );
    }

    /**
     * 设备更新设备组
     * @param deviceId  设备业务ID
     * @param groupId   设备组ID
     */
    public void moveToGroup(String deviceId, Long groupId) {
        // 1. 查设备
        Device device = deviceMapper.findByDeviceId(deviceId);

        // 2. 设备存在性校验
        if (device == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在！");
        }

        // 3. 设备存在但是已撤销了
        if (device.getStatus() == DeviceStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "设备已撤销！");
        }

        // 4. 检查设备组的存在性，若不存在方法会自动抛出404
        deviceGroupService.requireExists(groupId);

        // 5. 设备存在且激活，设备组存在
        int rows = deviceMapper.updateDeviceGroup(groupId, deviceId);

        // 设备组更新成功
        if (rows == 1) return;

        // 影响行数为 0，重查后判断原因（可能更新成功）
        if (rows == 0) {
            // 设备最新状态
            Device latest = deviceMapper.findByDeviceId(deviceId);

            // 设备不存在
            if (latest == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在！");
            }

            // 设备存在，但是已撤销
            if (latest.getStatus() == DeviceStatus.REVOKED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "设备已撤销！");
            }

            /**
             * 设备存在，并且处于激活状态，并且已经位于目标组
             * （主要是针对更新语句返回行数的处理）
             * 注意此处的比较不要使用“==”，而是使用equals，因为设备当前的groupId可能是null
             * 而这里的组 ID 是 Long 对象，用 == 比较两个 Long 时，判断的是它们是否指向同一个对象。
             * equals 用于比较数值，`Objects.equals` 还负责安全处理 null
             * */
            if (Objects.equals(latest.getGroupId(), groupId)) return;
        }

        // 其他未预期的结果
        throw new IllegalStateException("设备 " + deviceId + " 设备组更新失败，影响行数为 " + rows);
    }
}
