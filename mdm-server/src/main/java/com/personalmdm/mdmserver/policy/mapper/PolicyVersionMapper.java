package com.personalmdm.mdmserver.policy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PolicyVersionMapper {

    /**
     * 查询最新策略版本号
     * 利用 降序+数量限制
     * FOR UPDATE 表示查询的时候取得排他锁，持有到事务结束
     * @param policyId
     * @return
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
}
