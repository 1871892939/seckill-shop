package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.feign.ProductFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by lanxw
 */
@Service
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private ProductFeignApi productFeignApi;

    @Override
    public List<SeckillProductVo> queryByTime(Integer time) {
//        System.out.println("老"+time);
        List<SeckillProduct> seckillProductList = seckillProductMapper.queryCurrentlySeckillProduct(time);
        if (seckillProductList.size() == 0) {
//            System.out.println("seckillProductList.size()为空");
            return Collections.EMPTY_LIST;
        }
//        System.out.println("seckillProductList.size()不为空");
        List<Long> productIds = new ArrayList<>();
        for (SeckillProduct seckillProduct : seckillProductList) {
//            System.out.println("塞入一条seckill数据,塞入product的id:"+seckillProduct.getId());
            productIds.add(seckillProduct.getProductId());
        }
//        System.out.println(productIds);
        Result<List<Product>> result = productFeignApi.queryByIds(productIds);
//        System.out.println(result.getData());
        if (result == null || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        List<Product> productList = result.getData();

        Map<Long, Product> productMap = new HashMap<>();
        for(Product product:productList) {
            productMap.put(product.getId(), product);
        }
        int i = 0;
//        System.out.println("productMap大小是" + ++i);
        List<SeckillProductVo> seckillProductVoList = new ArrayList<>();
        for (SeckillProduct seckillProduct:seckillProductList) {
            SeckillProductVo vo = new SeckillProductVo();
            Product product = productMap.get(seckillProduct.getProductId());
            if (product != null) {
//                System.out.println("product不为空");
                BeanUtils.copyProperties(product, vo);
            } else {
                System.out.println("product为空");
                continue;
            }
            BeanUtils.copyProperties(seckillProduct, vo);
            vo.setCurrentCount(seckillProduct.getStockCount());
            seckillProductVoList.add(vo);
        }
        return seckillProductVoList;
    }

    @Override
    public SeckillProductVo find(Integer time, Long seckillId) {
        SeckillProduct seckillProduct = seckillProductMapper.getSeckillProductBySeckillId(seckillId);
        List<Long> productIds = new ArrayList<>();
        productIds.add(seckillProduct.getProductId());
        Result<List<Product>> result = productFeignApi.queryByIds(productIds);
        if (result == null || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        Product product = result.getData().get(0);
        SeckillProductVo vo = new SeckillProductVo();
        BeanUtils.copyProperties(product, vo);
        BeanUtils.copyProperties(seckillProduct, vo);
        vo.setCurrentCount(seckillProduct.getStockCount());
        return vo;
    }

    @Override
    public int decrStockCount(Long seckillId) {
        return seckillProductMapper.decrStock(seckillId);
    }

    @Override
    public SeckillProductVo findFromCache(Integer time, Long seckillId) {
//        System.out.println("新find"+ seckillId);
        String key = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        Object strObj = redisTemplate.opsForHash().get(key, String.valueOf(seckillId));
        SeckillProductVo vo = JSON.parseObject((String) strObj, SeckillProductVo.class);
        return vo;
    }

    @Override
    public List<SeckillProductVo> queryByTimeFromCache(Integer time) {
//        System.out.println("新query:"+time);
        String key = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        List<Object> objStrList = redisTemplate.opsForHash().values(key);
        List<SeckillProductVo> seckillProductVoList = new ArrayList<>();
        for (Object objStr:objStrList) {
            seckillProductVoList.add(JSON.parseObject((String)objStr, SeckillProductVo.class));
        }
        return seckillProductVoList;
    }

    @Override
    public void syncStockToRedis(Integer time, Long seckillId) {
        SeckillProduct seckillProduct = seckillProductMapper.getSeckillProductBySeckillId(seckillId);
        if (seckillProduct.getStockCount() > 0) {
            String key = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            redisTemplate.opsForHash().put(key, String.valueOf(seckillId), String.valueOf(seckillProduct.getStockCount()));
        }
    }

    @Override
    public void incrStockCount(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }
}