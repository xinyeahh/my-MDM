package com.personalmdm.mdmserver.policy.entity;

import java.time.LocalDateTime;

/**
 * 策略实体
 */
public class Policy {
    // 数据库内部id
    private Long id;

    // 策略名称
    private String policyName;

    // 策略类型
    private PolicyType policyType;

    // 策略创建时间
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
