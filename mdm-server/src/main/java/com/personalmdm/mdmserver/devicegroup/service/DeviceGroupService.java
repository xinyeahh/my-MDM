package com.personalmdm.mdmserver.devicegroup.service;

import com.personalmdm.mdmserver.devicegroup.dto.DeviceGroupCreateRequest;
import com.personalmdm.mdmserver.devicegroup.dto.DeviceGroupCreateResponse;
import com.personalmdm.mdmserver.devicegroup.entity.DeviceGroup;
import com.personalmdm.mdmserver.devicegroup.mapper.DeviceGroupMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class DeviceGroupService {
    // 声明依赖
    private final DeviceGroupMapper deviceGroupMapper;

    // 通过构造器注入依赖
    public DeviceGroupService(
            DeviceGroupMapper deviceGroupMapper
    ){
        this.deviceGroupMapper = deviceGroupMapper;
    }

    /**
     * 创建设备组
     * @param request   创建设备组请求 DTO
     * @return          创建设备组响应 DTO
     */
    public DeviceGroupCreateResponse create(DeviceGroupCreateRequest request) {

        // 对创建的设备组名称进行格式化处理，删除名称中的空格
        String groupName = request.groupName().strip(); // strip()只去掉首尾的空白

        // 创建设备组实体对象
        DeviceGroup deviceGroup = new DeviceGroup();

        // 设置组名
        deviceGroup.setGroupName(groupName);

        // 插入设备组
        int rows;   // 记录设备组插入结果
        try {
            rows = deviceGroupMapper.insert(deviceGroup);
        }catch (DuplicateKeyException e) {
            // 数据库内部主键由于自动自增不可能冲突，所以这里冲突的一定是设备组名称
            throw new ResponseStatusException(HttpStatus.CONFLICT, "设备组名称已存在！", e);
        }

        /**
         * 设备组插入异常情况处理
         * 由于设备组需要设备组id来构造成功响应，而就算插入成功，myBatis也有没把主键协会Java对象的风险，所以要额外检查设备组id是否为空
         *
         */
        if (rows != 1 || deviceGroup.getId() == null) {
            throw new IllegalStateException("创建设备组后未取得有效的插入结果");
        }

        // 创建成功，返回创建设备组响应DTO
        return new DeviceGroupCreateResponse(deviceGroup.getId(), deviceGroup.getGroupName());
    }

    /**
     * 主要提供目标组存在性检查,服务于设备变更设备组
     * @param groupId   设备组内部id
     */
    public void requireExists(Long groupId) {
        DeviceGroup group = deviceGroupMapper.findById(groupId);
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "目标设备组不存在！");
        }
    }
}
