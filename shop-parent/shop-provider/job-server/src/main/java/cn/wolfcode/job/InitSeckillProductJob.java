package cn.wolfcode.job;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.redis.JobRedisKey;
import cn.wolfcode.web.feign.SeckillProductFeignAPI;
import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Setter
@Getter
public class InitSeckillProductJob implements SimpleJob {
    @Autowired
    private SeckillProductFeignAPI seckillProductFeignAPI;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value("${jobCron.initSeckillProduct}")
    private String cron;

//    @Autowired
    public InitSeckillProductJob(SeckillProductFeignAPI seckillProductFeignAPI, StringRedisTemplate redisTemplate) {
        this.seckillProductFeignAPI = seckillProductFeignAPI;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void execute(ShardingContext shardingContext) {
//        System.out.println(shardingContext.getShardingParameter());
        String time = shardingContext.getShardingParameter();
        Result<List<SeckillProductVo>> result = seckillProductFeignAPI.queryByTimeForJob(Integer.parseInt(time));
        if (result == null || result.hasError()) {
            return;
        }
        List<SeckillProductVo> seckillProductVoList = result.getData();
        String key = JobRedisKey.SECKILL_PRODUCT_HASH.getRealKey(time);
        String seckillStockCountKey = JobRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(time);
        redisTemplate.delete(key);
        redisTemplate.delete(seckillStockCountKey);
        for (SeckillProductVo vo:seckillProductVoList) {
            redisTemplate.opsForHash().put(key, String.valueOf(vo.getId()), JSON.toJSONString(vo));
            redisTemplate.opsForHash().put(seckillStockCountKey, String.valueOf(vo.getId()), String.valueOf(vo.getStockCount()));
        }
    }
}
