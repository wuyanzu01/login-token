package com.demo.config;

import com.demo.intercetor.MyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class MvcConfig extends WebMvcConfigurationSupport {


    @Autowired
    private MyInterceptor myInterceptor;


    /**
     *
     * 功能描述:
     *  配置静态资源,避免静态资源请求被拦截
     * @date:
     * @param:
     * @return:
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        super.addResourceHandlers(registry);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myInterceptor)
                //addPathPatterns 用于添加拦截规则
                .addPathPatterns("/**")
                //excludePathPatterns 用于排除拦截
                //项目启动测试接口
                .excludePathPatterns("/","/error","/isLogin","/welcome","/static/**")
                .excludePathPatterns("/login")
                .excludePathPatterns("/index");
        super.addInterceptors(registry);
    }

}
