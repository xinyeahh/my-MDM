-- 策略表新增策略类型列
alter table policy
    add column policy_type
        varchar(50)
        not null
    after policy_name -- 把新增列放在policy name后面