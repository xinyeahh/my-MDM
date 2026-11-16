package com.personalmdm.mdmserver.policy.dto;

import com.personalmdm.mdmserver.policy.entity.PolicyType;

/**
 * 创建策略响应 DTO
 * @param policyId      数据库内部主键
 * @param policyName    策略名称
 * @param policyType    策略类型
 */
public record PolicyCreateResponse(
        Long policyId,
        String policyName,
        PolicyType policyType
) {
}
