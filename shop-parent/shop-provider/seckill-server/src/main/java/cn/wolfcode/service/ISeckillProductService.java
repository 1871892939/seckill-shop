package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;

/**
 * Created by lanxw
 */
public interface ISeckillProductService {
    /**
     * 查询秒杀列表数据
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTime(Integer time);

    SeckillProductVo find(Integer time, Long seckillId);

    int decrStockCount(Long seckillId);

    SeckillProductVo findFromCache(Integer time, Long seckillId);

    List<SeckillProductVo> queryByTimeFromCache(Integer time);

    void syncStockToRedis(Integer time, Long seckillId);

    void incrStockCount(Long seckillId);
}
