package com.daydreamdev.secondskill;

import com.daydreamdev.secondskill.common.utils.ScriptUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

import java.util.*;

/**
 * @author zhouhesheng
 * @email hzhou@ilegendsoft.com
 * @since 2022/9/1 17:05
 */
public class RedisTemplateTest {
  private Logger logger = LoggerFactory.getLogger(RedisTemplateTest.class);

  private static Integer maxTotal = 300;
  private static Integer maxIdle = 100;
  private static Integer maxWait = 10000;
  private static Boolean testOnBorrow = true;
  private static String redisIP = "127.0.0.1";
  private static Integer redisPort = 6379;

  private RedisTemplate<String, Object> redisTemplate;
  private DefaultRedisScript<String> script;


  @Bean
  JedisConnectionFactory jedisConnectionFactory() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setMaxTotal(maxTotal);
    config.setMaxIdle(maxIdle);
    config.setTestOnBorrow(testOnBorrow);
    config.setBlockWhenExhausted(true);
    config.setMaxWaitMillis(maxWait);
    final JedisConnectionFactory factory = new JedisConnectionFactory(config);
    JedisShardInfo shardInfo = new JedisShardInfo(redisIP, redisPort);
    factory.setShardInfo(shardInfo);
    return factory;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(jedisConnectionFactory());
    template.setEnableTransactionSupport(true);
    template.setKeySerializer(new StringRedisSerializer());
    template.afterPropertiesSet();

    return template;
  }

  @Before
  public void beforeFunction() {
    this.redisTemplate = redisTemplate();

    final String lua = ScriptUtil.getScript("secKill.lua");
    this.script = new DefaultRedisScript<>();
    this.script.setScriptText(lua);
    logger.info("Before Function");
  }

  @Test
  public void redisTestGet() {
    String key = "seckill:goodsStock:hi";
    if (redisTemplate.hasKey(key)) {
      final Object result = redisTemplate.opsForHash().get(key, "totalCount");
      logger.info("exists={}", result);
    } else {
      logger.info("not exists");
    }
  }


  @Test
  public void redisTestSet() {
    String key = "seckill:goodsStock:hi";
    Map<String, Integer> goods = new HashMap<>();
    goods.put("totalCount", 100);
    goods.put("initStatus", 1);
    goods.put("seckillCount", 0);
    redisTemplate.opsForHash().putAll(key, goods);
  }

  @Test
  public void redisTestLua() {
    String key = "seckill:goodsStock:hello";
    final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptText("return tonumber(ARGV[1])");
    redisScript.setResultType(Long.class);
    Long seckillCount = redisTemplate.execute(redisScript, Collections.singletonList(key), "1");
    logger.info("seckillCount={}", seckillCount);
  }

  @Test
  public void redisTestLua0() {
    final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptText("local times = redis.call('incr',KEYS[1]) if times == 1 then redis.call('expire',KEYS[1],ARGV[1]) end if times > 5 then return times end return -1");
    redisScript.setResultType(Long.class);
    List<String> list = new ArrayList<>();
    list.add("door3");
    Long result = redisTemplate.execute(redisScript, list, "60");
    logger.info("result={}", result);
  }
}
