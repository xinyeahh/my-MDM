-- 策略版本表，一个策略有多个版本
create table policy_version (
    id bigint not null auto_increment,

    -- 所属策略
    policy_id bigint not null,

    -- 策略版本号
    version_no int not null,

    -- JSON 类型校验 JSON 语法，具体配置的业务规则由 Service 校验
    config json not null,

    create_time datetime(6) not null default current_timestamp(6),

    -- 主键，唯一标识一条版本记录
    primary key (id),

    -- 唯一键， 保证同一策略的版本号不重复
    unique key uk_policy_version_number (policy_id, version_no),

    -- 唯一键，为后续复合外键提供对应的唯一键。
    unique key uk_policy_version_policy_id (policy_id, id),

    constraint fk_policy_version_policy
        foreign key (policy_id) references policy (id)
        on delete restrict
)
    engine InnoDB
    default character set = utf8mb4
    collate = utf8mb4_0900_ai_ci;
