package com.personalmdm.mdmserver.policy.entity;

import java.time.LocalDateTime;

/**
 * 策略版本实体
 */
public class PolicyVersion {
    // 数据库内部主键：版本记录的唯一身份
    private Long id;

    // 策略表的数据库内部主键
    private Long policyId;

    // 版本号：某项策略内部的版本号
    private Integer versionNo;

    // 版本配置
    private String config;

    // 创建时间
    private LocalDateTime createTime;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
