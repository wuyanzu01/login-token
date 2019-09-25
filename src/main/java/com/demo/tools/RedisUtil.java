package com.demo.tools;

import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class RedisUtil {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    //解锁结果的常量值
    private static final Long RELEASE_SUCCESS=1L;


    /**
     * 获取redis中的分布式锁
     * @param lockKey   key值
     * @param requestId 可为UUID，请求进来生成的UUID，作为value
     * @param expireTime    过期时间 单位为秒
     * @return  是否获取成功
     */
    public boolean tryGetDcsLock(String lockKey,String requestId,int expireTime){
        boolean result=redisTemplate.opsForValue().setIfAbsent(lockKey,requestId,expireTime,TimeUnit.SECONDS);
        return result;
    }


    /**
     * 释放分布式锁
     * @param lockKey   锁key
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public boolean releaseDcsLock(String lockKey,String requestId){
        //该段lua脚本的含义：首先获取锁对应的value值，检查是否与requestId相等，如果相等则删除锁（解锁）。
        //执行完了该脚本命令，采取执行其他的命令
        //使用lua脚本主要是为了确保原子性，要么成功要么失败。
        String script="if redis.call('get',KEYS[1]==ARGV[1] then return redis.call('del',KEYS[1) else return 0 end";
        DefaultRedisScript<Long> redisScript=new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        //KEYS[1]=是在lua中有一个全局变量KEYS数组，KEYS为执行命令的键名
        //ARGV[1]=是在lua中有一个全局参数ARGV数组，ARGV为执行命令的参数
        //Collections.singletonList主要用于一个元素的优化，不需要额外分配内存，可减少内存分配
        Object result=redisTemplate.execute(redisScript,Collections.singletonList(lockKey),Collections.singletonList(requestId));
        if(result.equals(RELEASE_SUCCESS)){
            return true;
        }
        return false;
    }


    /**
     * 指定缓存失效时间
     * @param key 键
     */
    public boolean expire(String key,long time){
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key,获取过期时间
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key){
        return redisTemplate.getExpire(key,TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return true 存在 false 不存在
     */
    public boolean hasKey(String key){

        try {
            return redisTemplate.hasKey(key);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }


    /**
     * 删除普通缓存
     * @param key 可以传一个值，或多个
     */
    public void del(String... key){
        if(key != null && key.length > 0){

            if(key.length == 1){
                redisTemplate.delete(key[0]);
            }else{
               redisTemplate.delete(CollectionUtils.arrayToList(key));
            }

        }

    }

    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public Object get(String key){
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }


    /**
     * 普通缓存放入
     * @param key 键
     * @param value 值
     * @return true or false
     */
    public boolean set(String key,Object value){
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0，将设置无限期
     * @return true or false
     */
    public boolean set(String key,Object value,long time){
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     * @param key 键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key,long delta){

        if(delta < 0){
            throw  new RuntimeException("递增因子必须大于0");
        }
        return  redisTemplate.opsForValue().increment(key,delta);
    }

    /**
     * 递减
     * @param key 键
     * @param delta 要减少几(大于0)
     * @return
     */
    public long decr(String key,long delta){

        if(delta < 0){
            throw  new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().decrement(key,delta);
    }

    /**
     * HashGet
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public <T>T hget(String key,String item){

        if(key == null){
            throw new RuntimeException("key not null");
        }else if(item == null){
            throw new RuntimeException("item not null");
        }

        return (T) redisTemplate.opsForHash().get(key,item);
    }


    /**
     * 获取hashkey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     */
    public Map hmget(String key){

        if(key == null){
            throw new RuntimeException("key not null");
        }

        return redisTemplate.opsForHash().entries(key);
    }

    /**
     *
     * @param key hasm键
     * @param match item正则表达式
     * @param count 每次扫描的记录数。值越小，扫描次数越过、越耗时。建议设置在1000-10000
     * @param <K>
     * @param <V>
     * @return
     */
    public <K,V>Map<K,V> hmgetByScan(String key,String match,Long count,Long size){

        if(key == null){
            throw new RuntimeException("key not null");
        }

        Map map = new HashMap();

        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(match).count(count).build();

        Cursor<Map.Entry<K, V>> cursor =
                redisTemplate.<K,V>opsForHash().scan(key,scanOptions);

        while (cursor.hasNext()){
            Map.Entry<K,V> entry =cursor.next();
            map.put(entry.getKey(),entry.getValue());
            if(map.size() >= size){
                break;
            }
        }

        closeCursor(cursor);

        return map;
    }


    /**
     * Hash
     * @param key 键
     * @param map 对应多个键值
     * @return true or false
     */
    public boolean hmset(String key,Map map){

        try {
            redisTemplate.opsForHash().putAll(key,map);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Hash 并设置时间
     * @param key 键
     * @param map 对应多个键值
     * @param time 时间(秒)
     * @return true or false
     */
    public boolean hmset(String key,Map map,long time){

        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key,time);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据，如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @return true or false
     */
    public boolean hset(String key,String item,Object value){

        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据，如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @param time 时间（秒） 注意：如果已存在的hash表有时间，这里将会替换原有的时间
     * @return true or false
     */
    public boolean hset(String key,String item,Object value,long time){

        try {
            redisTemplate.opsForHash().put(key,item,value);
            if(time > 0){
                expire(key,time);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }



    /**
     * 删除hash表中的指定项
     * @param key 键 不能为null
     * @param item 项 可以有多个,但不能为null
     */
    public void hdel(String key,Object...item){
        redisTemplate.opsForHash().delete(key,item);
    }


    /**
     * 删除整个hash表
     * @param key 键 不能为null
     */
    public void hmdel(String key){
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("*").count(1000).build();
        Cursor<Map.Entry<Object, Object>> cursor =
                redisTemplate.opsForHash().scan(key,scanOptions);
        while (cursor.hasNext()){
            Object item = cursor.next().getKey();
            hdel(key,item);
        }
        closeCursor(cursor);
    }



    /**
     * 判断hash表中是否有该项的值
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return true or false
     */
    public boolean hHasKey(String key,String item){
        return redisTemplate.opsForHash().hasKey(key,item);
    }

    /**
     * hash递增,如果不存在,就会创建一个，并把新增后的值返回
     * @param key 键
     * @param item 项
     * @param by 要增加几(大于0)
     * @return
     */
    public double hincr(String key,String item,double by){
        return redisTemplate.opsForHash().increment(key,item,by);
    }
    /**
     * hash递减
     * @param key 键
     * @param item 项
     * @param by 要减少几(大于0)
     * @return
     */
    public double hdecr(String key,String item,double by){
        return redisTemplate.opsForHash().increment(key,item,-by);
    }


    //----

    /**
     * 根据key获取set中的所有值
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key){

        try {
            return redisTemplate.opsForSet().members(key);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据value从一个set查询,是否存在
     * @param key 键
     * @param value 值
     * @return true or false
     */
    public boolean sHasKey(String key,Object value){

        try {
            return redisTemplate.opsForSet().isMember(key,value);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     * @param key 键
     * @param vlaues 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key,Object...vlaues){

        try {
            return redisTemplate.opsForSet().add(key, vlaues);
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     * @param key 键
     * @param time 时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key,long time,Object...values){

        try {
            Long count = sSet(key, values);
            if (count > 0) {
                expire(key, time);
            }
            return count;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     * @param key 键
     * @return 长度
     */
    public long sGetSetSize(String key){
        try {
            return redisTemplate.opsForSet().size(key);
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除值为value的
     * @param key 键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key,Object...values){

        try {
            long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取list缓存的内容
     * @param key 键
     * @param start 开始位置
     * @param end 结束位置 0 到 -1 代表获取所有
     * @return
     */
    public <T>List<T> lGet(String key,long start,long end){
        try{
            return (List<T>) redisTemplate.opsForList().range(key,start,end);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     * @param key 键
     * @return
     */
    public long lGetListSize(String key){
        try {
            Long count = redisTemplate.opsForList().size(key);
            return count == null ? 0:count;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 通过索引,获取list中的值
     * @param key 键
     * @param index 索引 index>=0时, 0 表头 1 第二个元素,依次类推；index < 0 时, -1 表尾 -2 倒数第二个元素,依次类推
     * @return
     */
    public<T> T lGetIndex(String key,long index){
        try {
            return (T) redisTemplate.opsForList().index(key, index);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将一个值插入到list的头部
     * @param key 键
     * @param value 值
     * @return
     */
    public boolean lSet(String key,Object value){
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将一个值插入到list的头部
     * @param key 键
     * @param value 值
     * @param time 时间（秒）
     * @return
     */
    public boolean lSet(String key,Object value,long time){

        try {
            redisTemplate.opsForList().rightPush(key,value);
            if(time > 0){
                expire(key,time);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param values 值
     * @return
     */
    public boolean lSet(String key,List<Object> values){
        try {
            redisTemplate.opsForList().rightPushAll(key,values);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param values 值
     * @param time 时间(秒)
     * @return
     */
    public boolean lSet(String key,List<Object> values,long time){

        try {
            lSet(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 根据索引修改list中的某条数据
     * @param key 键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key,long index,Object value){

        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     * @param key 键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key,long count,Object value){

        try{

            Long remove = redisTemplate.opsForList().remove(key,count,value);
            return remove;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }

    }



//    public void pipelined(String key,Map map){
//
//        byte[] bytes = key.getBytes();
//        redisTemplate.executePipelined(new RedisCallback<Object>() {
//            @Override
//            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
//
//                redisConnection.openPipeline();
//                redisConnection.hMSet(bytes,map);
//
//                return null;
//            }
//        });
//
//    }


    /**
     * 关闭cursor
     * @param cursor
     */
    public void closeCursor(Cursor cursor){

        if(cursor.isClosed()){return;}
        if(null != cursor){

            try {
                cursor.close();
            } catch (IOException e) {
                log.info("cursor close faill");
                e.printStackTrace();
            }

        }

    }

    /**
     *
     */
    public void convertAndSend(String chanel,Object message){
        redisTemplate.convertAndSend(chanel,message);
    }









}
