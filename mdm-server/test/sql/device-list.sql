SELECT device_id, device_name, model, os, status, create_time
FROM device
ORDER BY id DESC    -- 按id倒序排列
LIMIT 2             -- 最多返回两条
OFFSET 0;           -- 跳过前0条，再开始取


SELECT device_id, device_name, model, os, status, create_time
FROM device
ORDER BY id DESC
LIMIT 2             -- 最多返回2条
OFFSET 2;           -- 跳过前2条，再开始取

