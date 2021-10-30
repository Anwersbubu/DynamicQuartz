package com.yangjie.dynamicquartz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yangjie.dynamicquartz.mapper")
public class DynamicQuartzApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicQuartzApplication.class, args);
    }

}
