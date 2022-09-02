package com.daydreamdev.secondskill.controller;

import com.daydreamdev.secondskill.common.utils.ScriptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


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
  private static final String success = "SUCCESS";
  private static final String error = "ERROR";
  private static final String GOODS_CACHE = "seckill:goodsStock:";

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  private final DefaultRedisScript<Long> script;


  private String getCacheKey(String id) {
    return GOODS_CACHE.concat(id);
  }


  public SecKillController() {

    final String lua = ScriptUtil.getScript("secKill.lua");
    this.script = new DefaultRedisScript<>();
    this.script.setScriptText(lua);
    this.script.setResultType(Long.class);
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
    goods.put("seckillCount", 0);
    redisTemplate.opsForHash().putAll(key, goods);

    final Object result = redisTemplate.opsForHash().get(key, "totalCount");
    return success + ":" + result;
  }

  @RequestMapping(value = "secKill", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity<String> secKill(HttpServletRequest request, String id, int number) {
    String key = getCacheKey(id);
    Long seckillCount = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(number));
    if (seckillCount == 0) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    return ResponseEntity.ok(success + ":" + seckillCount);
  }
}
