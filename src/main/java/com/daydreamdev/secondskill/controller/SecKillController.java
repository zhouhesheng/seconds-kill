package com.daydreamdev.secondskill.controller;

import com.daydreamdev.secondskill.common.utils.ScriptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.JedisPoolConfig;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhouhesheng
 * @email hzhou@ilegendsoft.com
 * @since 2022/9/1 14:18
 */

@Slf4j
@Controller
@RequestMapping(value = "/secKill")
public class SecKillController {
  private static Integer maxTotal = 300;

  private static Integer maxIdle = 100;

  private static Integer maxWait = 10000;

  private static Boolean testOnBorrow = true;

  private static String redisIP = "127.0.0.1";
  private static Integer redisPort = 6379;

  private static final String success = "SUCCESS";
  private static final String error = "ERROR";
  private static final String GOODS_CACHE = "seckill:goodsStock:";
  private final RedisTemplate<String, Object> redisTemplate;
  private final DefaultRedisScript<Object> script;


  private String getCacheKey(String id) {
    return GOODS_CACHE.concat(id);
  }

  @Bean
  JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisIP, redisPort);
    return new JedisConnectionFactory(redisStandaloneConfiguration);
  }


  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(jedisConnectionFactory());
    template.setEnableTransactionSupport(true);
    template.afterPropertiesSet();

    return template;
  }

  public SecKillController() {
    this.redisTemplate = redisTemplate();

    final String lua = ScriptUtil.getScript("secKill.lua");
    this.script = new DefaultRedisScript<>();
    this.script.setScriptText(lua);
  }


  /**
   * 压测前先请求该方法，初始化缓存
   */
  @RequestMapping(value = "prepare", method = RequestMethod.POST)
  @ResponseBody
  public String prepare(HttpServletRequest request, String id, int totalCount) {
    String key = getCacheKey(id);
    Map<String, Integer> goods = new HashMap<>();
    goods.put("totalCount", totalCount);
    goods.put("initStatus", 1);
    goods.put("seckillCount", 0);
    redisTemplate.opsForHash().putAll(key, goods);
    return success;
  }

  @RequestMapping(value = "secKill", method = RequestMethod.POST)
  @ResponseBody
  public String secKill(HttpServletRequest request, String id, int number) {
    String key = getCacheKey(id);

    Object seckillCount = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(number));
    return success + ":" + seckillCount.toString();
  }
}
