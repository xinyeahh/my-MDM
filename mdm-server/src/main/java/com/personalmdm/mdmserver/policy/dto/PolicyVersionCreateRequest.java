package com.personalmdm.mdmserver.policy.dto;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

/**
 * 策略版本创建请求 DTO
 * 策略 ID 后续由请求路径提供，版本号由服务端分配，所以请求 DTO 只需要 config
 * @param config    策略版本配置
 *                  JsonNode 表示解析后的JSON数据，让 Service 可以区分字段值的类型
 *                  区别于实体中的 String config 用于保存 JSON 文本。
 */
public record PolicyVersionCreateRequest(
        @NotNull
        JsonNode config
) {
}
