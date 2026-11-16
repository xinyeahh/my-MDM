package com.personalmdm.mdmserver.policy.controller;

import com.personalmdm.mdmserver.policy.dto.PolicyCreateRequest;
import com.personalmdm.mdmserver.policy.dto.PolicyCreateResponse;
import com.personalmdm.mdmserver.policy.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/policy")
public class PolicyController {
    // 声明依赖
    private final PolicyService policyService;

    // 构造注入
    public PolicyController(PolicyService policyService){
        this.policyService  = policyService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyCreateResponse create(
            @Valid
            @RequestBody
            PolicyCreateRequest request
    ) {
        return policyService.create(request);
    }
}
