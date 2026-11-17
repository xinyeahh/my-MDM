package com.personalmdm.mdmserver.policy.mapper;

import com.personalmdm.mdmserver.policy.entity.PolicyVersion;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PolicyVersionMapper {

    /**
     * 查询最新策略版本，并加锁
     * @param policyId 所属策略 ID
     * @return 最新策略版本；尚无版本时返回 null
     */
    @Select("""
            select id, policy_id, version_no, config, create_time
            from policy_version
            where policy_id = #{policyId}
            order by version_no desc
            limit 1
            for update 
            """)
    PolicyVersion findLatestForUpdate (
        @Param("policyId")
        Long policyId
    );

    /**
     * 插入策略版本
     * @param version 策略版本
     * @return        插入影响行数
     */
    @Insert("""
            insert into policy_version (policy_id, version_no, config) 
            values (#{policyId}, #{versionNo}, #{config})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PolicyVersion version);
}
