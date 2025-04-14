package cn.wolfcode.web.controller;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.ISeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by lanxw
 * 秒杀商品信息查询
 */
@RestController
@RequestMapping("/seckillProduct")
@Slf4j
public class SeckillProductController {
    @Autowired
    private ISeckillProductService seckillProductService;

    /**
     * 线程500 循环次数10
     * qps1100 新4800
     * @param time
     * @return
     */
    @RequestMapping("/queryByTime")
    public Result<List<SeckillProductVo>> queryByTime(Integer time) {
        return Result.success(seckillProductService.queryByTimeFromCache(time));
    }

    /**
     * 线程500 循环次数10
     * qps1700 新4700
     * @param time
     * @return
     */
    @RequestMapping("/find")
    public Result<SeckillProductVo> find(Integer time, Long seckillId) {
        return Result.success(seckillProductService.findFromCache(time, seckillId));
    }
}
