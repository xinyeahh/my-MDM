-- # 创建设备表
create table device(
    -- ## 定义字段类型和属性：描述每个字段怎么存

    -- 数据库内部主键：bigint类型；非空；自增
    id bigint not null auto_increment,

    -- ### 业务标识

    -- 设备业务标识：varchar类型；ASCII字符集；按大小写精确比较排序；非空
    device_id varchar(40) character set ascii collate ascii_bin not null,

    -- 设备指纹：varchar类型；ASCII字符集；按大小写精确比较排序；非空
    fingerprint varchar(128) character set ascii collate ascii_bin not null,

    -- ### 设备属性

    -- 设备名称：varchar类型；非空
    device_name varchar(100) not null,

    -- 设备型号：varchar类型；非空
    model varchar(100) not null,

    -- 操作系统：varchar类型；非空
    os varchar(50) not null,

    -- 操作系统版本：varchar类型；非空
    os_version varchar(50) not null,

    -- 设备状态：varchar类型；非空
    status varchar(20) not null,

    -- ### 时间

    -- 创建时间：datetime类型（小数秒精度为6位）；未显示提供时，自动填充创建时间；非空
    create_time datetime(6) default current_timestamp(6) not null,

    -- ## 表级约束：规定数据必须满足什么条件

    -- 指定id为主键：唯一；非空
    primary key (id),

    -- ### 唯一索引
    -- 指定device_id为唯一索引：唯一
    unique key uk_device_device_id(device_id),

    -- 指定fingerprint为唯一索引：唯一
    unique key uk_device_fingerprint(fingerprint)
)
    -- ## 表选项 table options：规定整张表以什么方式存储和处理字符

    -- 指定表使用：事务型存储引擎 InnoDB
    engine=InnoDB
    -- 指定表使用：默认字符集 utf8mb4
    default charset=utf8mb4;

-- # 建表后验证

-- ## 查看 device 表实际创建时的完整定义
show create table device;

-- ## 统计 device 表中有多少条记录
select count(*)     -- 统计符合条件的记录数
    as device_count -- 给查询结果设置别名
from device;

-- ## 查看具体数据
select *
from device;