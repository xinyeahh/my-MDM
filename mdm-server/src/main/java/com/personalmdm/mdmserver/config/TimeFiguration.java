package com.personalmdm.mdmserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeFiguration {

    @Bean   // 把方法返回的对象交给spring管理
    public Clock clock(){
        return Clock.systemDefaultZone();
    }
}
