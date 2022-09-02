package com.daydreamdev.secondskill;

import com.daydreamdev.secondskill.common.utils.ScriptUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;

/**
 * @author zhouhesheng
 * @email hzhou@ilegendsoft.com
 * @since 2022/9/1 17:05
 */
public class RedisTemplateTest {
  private Logger logger = LoggerFactory.getLogger(RedisTemplateTest.class);

  private RedisTemplate<String, Object> redisTemplate;
  private RedisScript<Long> script;


  @Bean
  JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
    return new JedisConnectionFactory(config);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(jedisConnectionFactory());
    template.setEnableTransactionSupport(true);
    template.setDefaultSerializer(new StringRedisSerializer());
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    template.afterPropertiesSet();
    return template;
  }

  @Before
  public void beforeFunction() {
    this.redisTemplate = redisTemplate();
    final String lua = ScriptUtil.getScript("secKill.lua");
    this.script = RedisScript.of(lua, Long.class);
    logger.info("Before Function");
  }

  @Test
  public void redisTestGet() {
    String key = "seckill:goodsStock:hello";
    if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
      final Object result = redisTemplate.opsForHash().get(key, "totalCount");
      logger.info("exists={}", result);
    } else {
      logger.info("not exists");
    }
  }


  @Test
  public void redisTestSet() {
    String key = "seckill:goodsStock:hello";
    Map<String, Integer> goods = new HashMap<>();
    goods.put("totalCount", 100);
    goods.put("seckillCount", 0);
    redisTemplate.opsForHash().putAll(key, goods);
  }

  @Test
  public void redisTestLua2() {
    String key = "seckill:goodsStock:hello";
    Long value = redisTemplate.execute(this.script, Collections.singletonList(key), "9");
    logger.info("value={}", value);
  }

  @Test
  public void redisTestLua1() {
    String key = "hello-lua";
    final RedisScript<Long> redisScript = RedisScript.of("return 2*tonumber(ARGV[1])", Long.class);
    Long value = redisTemplate.execute(redisScript, Collections.singletonList(key), "11");
    logger.info("value={}", value);
  }

  @Test
  public void redisTestLua0() {
    final RedisScript<Long> redisScript = RedisScript.of("local times = redis.call('incr',KEYS[1]) if times == 1 then redis.call('expire',KEYS[1],ARGV[1]) end return times", Long.class);
    List<String> list = new ArrayList<>();
    list.add("door");
    Long result = redisTemplate.execute(redisScript, list, "60");
    logger.info("result={}", result);
  }
}
