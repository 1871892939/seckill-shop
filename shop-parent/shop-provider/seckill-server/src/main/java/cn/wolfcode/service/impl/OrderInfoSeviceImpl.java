package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by wolfcode-lanxw
 */
@Service
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private RefundLogMapper refundLogMapper;

    @Override
    public OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId) {
        return orderInfoMapper.findByPhoneAndSeckillId(phone, seckillId);
    }

    @Override
    @Transactional
    public OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo) {
        int effectCount = seckillProductService.decrStockCount(seckillProductVo.getId());
        if (effectCount==0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        OrderInfo orderInfo = createOrderInfo(phone, seckillProductVo);
        String orderSetKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillProductVo.getId()));
        redisTemplate.opsForSet().add(orderSetKey, phone);
        return orderInfo;
    }

    @Override
    public OrderInfo findByOrderNo(String orderNo) {
        return orderInfoMapper.find(orderNo);
    }

    private OrderInfo createOrderInfo(String phone, SeckillProductVo seckillProductVo) {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(seckillProductVo, orderInfo);
        orderInfo.setUserId(Long.parseLong(phone));
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(1L);
        orderInfo.setSeckillDate(seckillProductVo.getStartDate());
        orderInfo.setSeckillTime(seckillProductVo.getTime());
        orderInfo.setOrderNo(String.valueOf(IdGenerateUtil.get().nextId()));
        orderInfo.setSeckillId(seckillProductVo.getId());
        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }
}
