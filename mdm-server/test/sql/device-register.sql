-- 验证设备是否成功注册
select *
from device
where fingerprint = 'sim-install-001';

-- 验证设备注册幂等性
select count(*)
from device
where fingerprint = 'sim-install-001';

-- 验证设备注册并发处理能力
select device_id, fingerprint
from device
where fingerprint = 'sim-concurrent-f2df1417-affc-4f55-a12a-eae3d4caa19a'

