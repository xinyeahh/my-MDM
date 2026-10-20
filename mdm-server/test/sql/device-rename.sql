-- 确认数据库驱动返回的是“匹配行数”还是“实际修改行数”
update device
set device_name = 'sim-install-007'
where device_id = 'dev_5037fd1b-9be3-4d14-87e8-7d3a339237db'