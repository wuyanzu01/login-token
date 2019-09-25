package com.demo.service;

import com.demo.entity.User;

import java.util.Map;

public interface UserService {

    Map<String,Object> isLogin(String userName,String userPass);

    User getUserByName(String userName);
}
