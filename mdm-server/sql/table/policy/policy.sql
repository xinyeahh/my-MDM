-- 策略表
create table policy (
    id bigint not null auto_increment,

    -- 策略名称，唯一
    policy_name varchar(100) not null,

    -- 创建时间
    create_time datetime(6) not null default current_timestamp(6),

    -- 主键
    primary key (id),

    -- 唯一键
    unique key uk_policy_policy_name (policy_name)
)
    engine = InnoDB
    -- 字符集
    default character set = utf8mb4
    -- 排序规则
    collate = utf8mb4_0900_ai_ci;
