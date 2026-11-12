-- 设备组数据库表

create table device_group (

    # 数据库内部主键
    id bigint not null auto_increment,

    # 设备组名称
    group_name varchar(100) not null,

    # 创建时间
    create_time datetime(6) not null default current_timestamp(6),

    # 指定id为主键
    primary key (id),

    # 设备组名称唯一
    unique key uk_device_group_group_name(group_name)
)
    engine = InnoDB
    default character set = utf8mb4;

-- 设备数据库表引入外键 group_id
alter table device
    # 新增设备组id列，初始化为null，允许为空
    add column group_id bigint null,

    # 新增外键约束
    add constraint fk_device_group
        foreign key (group_id) references device_group(id)
        on DELETE set null;