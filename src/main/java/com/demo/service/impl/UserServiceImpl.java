package com.demo.service.impl;

import com.demo.entity.User;
import com.demo.mapper.UserMapper;
import com.demo.service.UserService;
import com.demo.tools.JwtTokenUtil;
import com.demo.tools.RedisUtil;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Map<String,Object> isLogin(String userName,String userPass) {
        Map<String,Object> resultMap=new HashMap<>();
        resultMap.put("code",0);
        User user=userMapper.selectByName(userName);
        System.out.println(user);
        if(user!=null){
            if(user.getUserpassword().equals(userPass)){
                //登录成功，在这生成token
                try{
                    //token设置30分钟的有效期
                    String token=JwtTokenUtil.sign(user,30*60*1000,user.getUsername(),user.getUserpassword());
                    resultMap.put("token",token);
                    redisUtil.set("token:"+token,token,60*60); //redis中设置60分钟有效期
                }catch(Exception e){
                    log.error(Throwables.getStackTraceAsString(e));
                    return resultMap;
                }
                resultMap.put("code",1);
            }
        }
        return resultMap;
    }

    @Override
    public User getUserByName(String userName) {
        return userMapper.selectByName(userName);
    }
}
