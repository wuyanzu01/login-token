package com.demo.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.demo.entity.User;
import com.demo.service.UserService;
import com.demo.tools.*;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class LogingController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;


    @PostMapping("/isLogin")
    public Object isLogin(HttpServletRequest request){
        Map<String,Object> resultMap=new HashMap<>();
        String userName=request.getParameter("username");
        String userPass=request.getParameter("userPass");
        if(StringUnits.isEmpty(userName) || StringUnits.isEmpty(userPass)){
            resultMap.put("code",2);    //参数有误
            return resultMap;
        }
        resultMap=userService.isLogin(userName,userPass);
        return resultMap;
    }


    @RequestMapping("/getUserInfo")
    public Object getUserInfo(HttpServletRequest request){
        String token=request.getHeader("Authorization");
        Map<String,Object> resultMap=new HashMap<>();
        if(StringUnits.isEmpty(token)){
            resultMap.put("code",2);
        }
        User user= null;
        try{
            user=JwtTokenUtil.unsign(token);
            resultMap.put("data",userService.getUserByName(user.getUsername()));
        }catch(Exception e){
            Throwables.getStackTraceAsString(e);
            return resultMap;
        }
        resultMap.put("code",1);
        return resultMap;
    }


    @GetMapping("/loginOut")
    public Object loginOut(HttpServletRequest request){
        Map<String,Object> resultMap=new HashMap<>();
        //获取到token的有效期，从redis删除，获取到token的有效期，放入至黑名单redis中
        String token=request.getHeader("Authorization");
        redisUtil.del("token:"+token);
        try{
            DecodedJWT decodedJWT=JwtTokenUtil.getJwt(token);
            Date exp=decodedJWT.getExpiresAt();
            redisUtil.set("blackToken:"+token,token,DateUtils.getSecond(new Date(),exp));
            resultMap.put("code",1);
        }catch(Exception e){
            Throwables.getStackTraceAsString(e);
        }
        return resultMap;
    }

}
