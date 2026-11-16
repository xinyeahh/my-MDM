package com.personalmdm.mdmserver.policy.service;

import com.personalmdm.mdmserver.policy.dto.PolicyCreateRequest;
import com.personalmdm.mdmserver.policy.dto.PolicyCreateResponse;
import com.personalmdm.mdmserver.policy.entity.Policy;
import com.personalmdm.mdmserver.policy.mapper.PolicyMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PolicyService {
    private final PolicyMapper policyMapper;

    public PolicyService (PolicyMapper policyMapper){
        this.policyMapper = policyMapper;
    }

    /**
     * 创建策略
     * @param request   创建策略请求 DTO
     * @return          创建策略响应 DTO
     */
    @Transactional
    public PolicyCreateResponse create(PolicyCreateRequest request) {
        // 1. 去掉名称首尾的空白
        String name = request.policyName().strip();

        // 2. 创建Policy，设置名称和类型
        Policy policy = new Policy();
        policy.setPolicyName(name);
        policy.setPolicyType(request.policyType());

        // 3. 插入数据库，捕获名称重复产生的 DuplicateKeyException，返回409
        int rows;
        try{
            rows = policyMapper.insert(policy);
        }catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "策略名称重复！", exception);
        }

        // 4. 检查影响行数为 1， 回填ID非null
        if (rows != 1 || policy.getId() == null) {
            throw new IllegalStateException("创建策略后未取得有效的插入结果");
        }

        return new PolicyCreateResponse(policy.getId(), policy.getPolicyName(), policy.getPolicyType());
    }
}
