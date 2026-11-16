-- 设备组-策略关联表
create table device_group_policy (
    -- 组id
    group_id bigint not null,

    -- 策略id
    policy_id bigint not null,

    -- 策略版本id
    version_id bigint not null,

    -- 复合主键
    primary key (group_id, policy_id),

    -- 外键：存在策略关联时，禁止删除设备组
    constraint fk_device_group_policy_group_id
        foreign key (group_id)
            references device_group (id)
            on delete restrict,

    -- 复合外键，保证策略和版本不混乱
    constraint fk_device_group_policy_policy_id
        foreign key (policy_id, version_id)
            references policy_version (policy_id, id)
            on delete restrict
)
    engine InnoDB
    default character set = utf8mb4
    collate = utf8mb4_0900_ai_ci;
