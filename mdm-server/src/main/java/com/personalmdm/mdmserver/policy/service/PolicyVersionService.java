package com.personalmdm.mdmserver.policy.service;

import com.personalmdm.mdmserver.policy.mapper.PolicyMapper;
import com.personalmdm.mdmserver.policy.mapper.PolicyVersionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minLength 必须时有效整数");
        }

        int minLength = minLengthNode.intValue();

        if (minLength < 6 || minLength > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minLength 必须时有效整数");
        }
    }


}
