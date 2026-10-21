package com.personalmdm.mdmserver.device.service;

import com.personalmdm.mdmserver.device.dto.*;
import com.personalmdm.mdmserver.device.entity.Device;
import com.personalmdm.mdmserver.device.entity.DeviceStatus;
import com.personalmdm.mdmserver.device.mapper.DeviceMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * 构造方法
     * 构造器注入：Spring 创建 DeviceService 时，把 MyBatis 提供的 DeviceMapper 对象传进来。只有一个构造方法时，无须写 @Autowired。
     * 当 Spring 扫描到这个 Service，并且容器中存在可注入的 DeviceMapper Bean 时，Spring 就可以调用这个构造器，把 Mapper 传进来。
     * @param deviceMapper
     */
    public DeviceService(DeviceMapper deviceMapper){
        this.deviceMapper = deviceMapper;
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
            return new DeviceRegisterResponse(existing.getDeviceId());
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
        try {
            // 将设备插入数据库，插入本身就是原子操作
            deviceMapper.insert(device);
        }catch (DuplicateKeyException e){
            // 查询同一个 fingerprint
            Device registered = deviceMapper.findByFingerprint(request.getFingerprint());

            // 判断是否已有注册身份
            if(registered != null){
                // 已有注册身份，返回对应的设备ID
                return new DeviceRegisterResponse(registered.getDeviceId());
            }

            /**
             * 没有注册身份，重新抛出原异常
             * quest:为什么捕获异常后不能直接认定是重复设备？
             * ans: 因为表里还有 device_id 的唯一约束，DuplicateKeyException 本身并不保证冲突来自 fingerprint。
             */
            throw e;
        }

        // 注册成功，返回设备ID
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
        return new DeviceDetailResponse(
                device.getDeviceId(),
                device.getDeviceName(),
                device.getModel(),
                device.getOs(),
                device.getOsVersion(),
                device.getStatus().name(),
                device.getCreateTime()
        );
    }

    /**
     * 返回设备列表
     * @param page  页码
     * @param size  单词实际返回的条数
     * @return      设备列表页面响应 DTO
     */
    public DeviceListPageResponse list(int page, int size, String os){
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
        long total = deviceMapper.countAllByOs(normalizedOs);

        // 设备列表获取
        List<Device> deviceList = deviceMapper.findPageByOs(size, offset, normalizedOs);

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
}
