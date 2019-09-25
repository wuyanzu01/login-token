package com.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication
@Controller
@MapperScan("com.demo.mapper")
public class LoginTokenApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginTokenApplication.class, args);
    }


    @RequestMapping({"","/login"})
    public String login(){
        return "login";
    }


    @RequestMapping("/index")
    public String Index(){
        return "index";
    }


    @RequestMapping("/welcome")
    public String welcome(){
        return "welcome";
    }
}