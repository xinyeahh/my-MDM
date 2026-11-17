package com.personalmdm.mdmserver.policy.mapper;

import com.personalmdm.mdmserver.policy.entity.Policy;
import org.apache.ibatis.annotations.*;

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


    /**
     * 通过 id 查询策略
     * FOR UPDATE 表示查询的时候取得排他锁，持有到事务结束
     * @param policyId  版本所属策略id
     * @return          策略
     */
    @Select("""
            select id, policy_name, policy_type, create_time
            from policy
            where id = #{policyId}
            for update
            """)
    Policy findByIdForUpdate(
            @Param("policyId")
            Long policyId
    );
}

