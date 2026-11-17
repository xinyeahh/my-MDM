package com.personalmdm.mdmserver.policy.dto;

import tools.jackson.databind.JsonNode;

/**
 * 创建策略版本响应 DTO
 * @param versionId 策略版本表内部数据库id
 * @param policyId  所属策略id
 * @param versionNo 策略版本号
 * @param config    策略版本配置
 */
public record PolicyVersionCreateResponse(
        Long versionId,
        Long policyId,
        Integer versionNo,
        JsonNode config
) {
}
