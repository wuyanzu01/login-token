package com.demo.intercetor;

import com.demo.entity.User;
import com.demo.tools.JwtTokenUtil;
import com.demo.tools.RedisUtil;
import com.demo.tools.ResPrint;
import com.demo.tools.StringUnits;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.MimeHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;

@Component
@Slf4j
public class MyInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        try{
            String token=request.getHeader("Authorization");
            if(StringUnits.isEmpty(token)){
                ResPrint.print(500,"您未登录，请重新登录",response);
                return false;
            }
            //判断黑名单中的token是否包含(主要是去除注销过得token)
            Object blackToken=redisUtil.get("blackToken:"+token);
            if(blackToken!=null){
                ResPrint.print(500,"您未登录，请重新登录",response);
                return false;
            }
            //判断token是否有效
            if(JwtTokenUtil.isOverdue(token)){
                Object redisToken=redisUtil.get("token:"+token);
                if(redisToken==null){
                    ResPrint.print(500,"您登录已失效，请重新登录",response);
                    return false;
                }else{
                    //添加分布式锁，防止异步后续请求挂掉，保证所有请求拿到的结果都是一致的。
                    //分布式锁主要作让后续请求能拿到最新的token，防止生成更多的token到redis中
                    boolean result=redisUtil.tryGetDcsLock("dcsLockToken:"+token,token,3*60);
                    if(result){ //成功拿到锁
                            //redis还存在着此token，则生成新的token
                            User user=(User)JwtTokenUtil.unsign(redisToken.toString());
                            String newToken=JwtTokenUtil.sign(User.class,30*60*1000,user.getUsername(),user.getUserpassword());
                            redisUtil.set("token:"+newToken,newToken,60*60);
                            //把旧的token放入至redis中，以免后续携带旧token请求能及时获取新的token(只限3分钟)
                            redisUtil.set("oldToken:"+token,newToken,3*60);
                            response.setHeader("Authorization",newToken);
                            reflectSetparam(request,"Authorization",newToken);  //给request重新设置header参数
                    }else{
                            //如果未拿到锁则通过旧Token拿到最新的token
                            Object resToken=redisUtil.get("oldToken:"+token);
                            if(resToken!=null){
                                String newToken=resToken.toString();
                                response.setHeader("Authorization",newToken);
                                reflectSetparam(request,"Authorization",newToken);  //给request重新设置header参数
                            }else{
                                //如果oldToken已过期，则告知用户登录失效。
                                ResPrint.print(500,"您登录已失效，请重新登录",response);
                                return false;
                        }
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            log.error(Throwables.getStackTraceAsString(e));
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex){

    }

    /**
     * 修改header信息，key-value键值对儿加入到header中
     * @param request
     * @param key
     * @param value
     */
    private void reflectSetparam(HttpServletRequest request,String key,String value){
        Class<? extends HttpServletRequest> requestClass = request.getClass();
        try {
            Field request1 = requestClass.getDeclaredField("request");
            request1.setAccessible(true);
            Object o = request1.get(request);
            Field coyoteRequest = o.getClass().getDeclaredField("coyoteRequest");
            coyoteRequest.setAccessible(true);
            Object o1 = coyoteRequest.get(o);
            Field headers = o1.getClass().getDeclaredField("headers");
            headers.setAccessible(true);
            MimeHeaders o2 = (MimeHeaders)headers.get(o1);
            o2.addValue(key).setString(value);
        } catch (Exception e) {
            Throwables.getStackTraceAsString(e);
        }
    }
}
