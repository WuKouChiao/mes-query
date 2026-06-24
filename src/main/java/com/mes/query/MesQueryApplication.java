package com.mes.query;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mes.query.mapper")
public class MesQueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MesQueryApplication.class, args);
    }
}
