-- 新增设备心跳字段，可为空
alter table device add column last_heartbeat_time datetime(6) null