package com.personalmdm.mdmserver.devicegroup.entity;

public class DeviceGroup {
    // 设备组id，由mysql生成
    private Long id;

    // 设备组名称：创建组时设置
    private String groupName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
