package com.personalmdm.mdmserver.policy.mapper;

import com.personalmdm.mdmserver.policy.entity.Policy;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface PolicyMapper {

    /**
     * 创建策略
     * @param policy    要新增的策略
     * @return          数据库操作执行结果，匹配行数
     */
    @Insert("""
            insert into policy (policy_name, policy_type)
            values (#{policyName}, #{policyType})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")   // 响应需要返回生成的policyId
    int insert(Policy policy);
}

