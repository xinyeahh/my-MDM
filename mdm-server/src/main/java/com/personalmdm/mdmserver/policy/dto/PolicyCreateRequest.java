package com.personalmdm.mdmserver.policy.dto;

import com.personalmdm.mdmserver.policy.entity.PolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建策略请求 DTO
 * @param policyName
 * @param policyType
 */
public record PolicyCreateRequest(
        @NotBlank
        @Size(max = 100)
        String policyName,
        @NotNull
        PolicyType policyType
) {
}
