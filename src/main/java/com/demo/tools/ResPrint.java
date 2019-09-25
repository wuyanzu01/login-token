package com.demo.tools;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class ResPrint {

    public static void print(Integer code, String msg, HttpServletResponse response){
        response.setContentType("application/json; charset=utf-8");
        response.setCharacterEncoding("utf-8");
        Map<String,Object> resultMap=new HashMap<String,Object>();
        resultMap.put("code",code);
        resultMap.put("msg",msg);
        try{
            response.getWriter().print(JsonUtil.mapToJson(resultMap));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void alertMsg(String message,String url,HttpServletResponse response){
        response.setContentType("application/json; charset=utf-8");
        response.setCharacterEncoding("utf-8");
        try{
            response.getWriter().print("alert('"+message+"');localtion.href='"+url+"'");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
