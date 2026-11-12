-- 设备状态历史表：主要对设备状态的变更进行记录
create table device_status_history(
    # 历史的内部主键
    id bigint not null auto_increment,
    # 标识发生变化的设备
    device_id varchar(40) character set ascii collate ascii_bin not null,
    # 变化前的状态
    from_status varchar(20) not null,
    # 变化后的状态
    to_status varchar(20) not null,
    # 变化发生的时间
    occurred_at datetime(6) not null,

    # 主键
    primary key (id),
    # 为 device_id 添加普通索引，方便查询某台设备的历史
    key idx_device_status_history_device_id (device_id),
    # 引用 device.device_id 外键
    constraint fk_device_status_history_device
        foreign key (device_id) references device(device_id)
            # 保证插入历史的时候设备必须存在，设备有历史的时候拒绝删除设备
            on delete restrict
)
    engine = InnoDB
    default character set = utf8mb4;


