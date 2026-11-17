package com.personalmdm.mdmserver.policy.mapper;

import com.personalmdm.mdmserver.policy.entity.PolicyVersion;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PolicyVersionMapper {

    /**
     * 查询最新策略版本号
     * 利用 降序+数量限制
     * FOR UPDATE 表示查询的时候取得排他锁，持有到事务结束
     * @param policyId
     * @return 最新版本号；尚无版本时返回 null
     */
    @Select("""
            select version_no
            from policy_version
            where policy_id = #{policyId}
            order by version_no desc
            limit 1
            for update 
            """)
    Integer findLatestVersionNoForUpdate (
        @Param("policyId")
        Long policyId
    );

    /**
     * 插入策略版本
     * @param version 策略版本
     * @return
     */
    @Insert("""
            insert into policy_version (policy_id, version_no, config) 
            values (#{policyId}, #{versionNo}, #{config})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PolicyVersion version);
}
