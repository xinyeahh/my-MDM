package com.personalmdm.mdmserver.policy.service;

import com.personalmdm.mdmserver.policy.dto.PolicyCreateResponse;
import com.personalmdm.mdmserver.policy.dto.PolicyVersionCreateRequest;
import com.personalmdm.mdmserver.policy.dto.PolicyVersionCreateResponse;
import com.personalmdm.mdmserver.policy.entity.Policy;
import com.personalmdm.mdmserver.policy.entity.PolicyType;
import com.personalmdm.mdmserver.policy.entity.PolicyVersion;
import com.personalmdm.mdmserver.policy.mapper.PolicyMapper;
import com.personalmdm.mdmserver.policy.mapper.PolicyVersionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


@Service
public class PolicyVersionService {
    // 声明依赖
    private final PolicyMapper policyMapper;
    private final PolicyVersionMapper policyVersionMapper;
    private final ObjectMapper objectMapper;

    // 构造器注入依赖
    public PolicyVersionService(
            PolicyMapper policyMapper,
            PolicyVersionMapper policyVersionMapper,
            ObjectMapper objectMapper
    ){
        this.policyMapper = policyMapper;
        this.policyVersionMapper = policyVersionMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 校验密码配置
     *
     * @param config 配置
     *
     * isObject()：确认配置是 {...}。
     * size()、has()：要求恰好只有 minLength 一个字段。
     * isIntegralNumber()：拒绝字符串、小数和 null。
     * canConvertToInt()：拒绝超过 Java int 范围的巨大整数。
     */
    private void validatePasswordConfig(JsonNode config) {
        // 确认配置时Json
        if (config == null || !config.isObject()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "config 必须是JSON对象"
            );
        }

        // 验证恰好只有 minLength 一个字段
        if (config.size() != 1 || !config.has("minLength")){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "密码配置只能包含 minLength"
            );
        }

        JsonNode minLengthNode = config.get("minLength");

        // 拒绝字符串、小数和null，拒绝超过Java int 范围的巨大整数
        if (!minLengthNode.isIntegralNumber() || !minLengthNode.canConvertToInt()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minLength 必须是有效整数");
        }

        int minLength = minLengthNode.intValue();

        if (minLength < 6 || minLength > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minLength 必须在 6～64 之间");
        }
    }

    /**
     * 创建策略版本
     * @param policyId  所属策略
     * @param request   创建策略版本请求 DTO
     * @return          创建策略版本响应 DTO
     */
    @Transactional
    public PolicyVersionCreateResponse create(
            Long policyId,
            PolicyVersionCreateRequest request
    ){
        // 校验策略存在性：先锁策略
        Policy policy = policyMapper.findByIdForUpdate(policyId);

        if (policy == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "策略不存在");
        }

        // 确认策略类型，并校验配置参数
        if(policy.getPolicyType() == PolicyType.PASSWORD) {
            validatePasswordConfig(request.config());
        }

        // 校验配置是否已存在,如果已有版本，解析旧配置并比较，相同则抛 409：读取最新版本，并上锁
        PolicyVersion latest = policyVersionMapper.findLatestForUpdate(policyId);
        if (latest != null) {
            // 先将配置解析成 JsonNode
            JsonNode latestConfig = objectMapper.readTree(latest.getConfig());

            // 和请求中的配置进行比较
            if (latestConfig.equals(request.config())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "配置与最新版本相同，无需创建新版本"
                );
            }
        }

        // 分配版本号，创建 PolicyVersion
        int versionNo = (latest == null) ? 1 : Math.incrementExact(latest.getVersionNo());
        String configTxt = objectMapper.writeValueAsString(request.config());

        // 准备要插入的策略版本对象
        PolicyVersion newVersion = new PolicyVersion();
        newVersion.setPolicyId(policyId);
        newVersion.setVersionNo(versionNo);
        newVersion.setConfig(configTxt);

        // 插入并检查影响行数为 1、回填 ID 非空，否则抛 IllegalStateException。
        int rows = policyVersionMapper.insert(newVersion);
        if (rows != 1 || newVersion.getId() == null) {
            throw new IllegalStateException("插入新策略版本失败");
        }

        return new PolicyVersionCreateResponse(newVersion.getId(), policyId, versionNo, request.config());
    }
}
