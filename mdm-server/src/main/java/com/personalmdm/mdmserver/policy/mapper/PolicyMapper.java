package com.personalmdm.mdmserver.policy.mapper;

import com.personalmdm.mdmserver.policy.entity.Policy;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface PolicyMapper {

    @Insert("""
            insert into policy (policy_name, policy_type)
            values (#{policyName}, #{policyType})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")   // 响应需要返回生成的policyId
    int insert(Policy policy);
}

