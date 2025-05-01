package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.feign.IntegralFeignApi;
import cn.wolfcode.web.feign.PayFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private PayFeignApi payFeignApi;
    @Autowired
    private IntegralFeignApi integralFeignApi;

    @Override
    public OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId) {
        return orderInfoMapper.findByPhoneAndSeckillId(phone, seckillId);
    }

    @Override
    @Transactional
    public OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo) {
//        int i = 1/0;
        int effectCount = seckillProductService.decrStockCount(seckillProductVo.getId());
        if (effectCount==0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        OrderInfo orderInfo = createOrderInfo(phone, seckillProductVo);

        return orderInfo;
    }

    @Override
    public OrderInfo findByOrderNo(String orderNo) {
        String orderHashKey = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        String objStr = (String) redisTemplate.opsForHash().get(orderHashKey, orderNo);
        return JSON.parseObject(objStr, OrderInfo.class);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        System.out.println("timeout cancel order business start");
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        if (OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            int effectCount = orderInfoMapper.updateCancelStatus(orderNo, OrderInfo.STATUS_TIMEOUT);
            if (effectCount == 0) {
                return;
            }

            seckillProductService.incrStockCount(orderInfo.getSeckillId());
            seckillProductService.syncStockToRedis(orderInfo.getSeckillTime(), orderInfo.getSeckillId());
        }

        System.out.println("timeout cancel order business end");
    }

    @Value("${pay.returnUrl}")
    private String returnUrl;
    @Value("${pay.notifyUrl}")
    private String notifyUrl;
    @Override
    public Result<String> payOnline(String orderNo) {
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        if (OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            PayVo vo = new PayVo();
            vo.setOutTradeNo(orderNo);
            vo.setTotalAmount(String.valueOf(orderInfo.getIntergral()));
            vo.setSubject(orderInfo.getProductName());
            vo.setBody(orderInfo.getProductName());
            vo.setReturnUrl(returnUrl);
            vo.setNotifyUrl(notifyUrl);
            Result<String> result = payFeignApi.payOnline(vo);
            return result;
        }
        return Result.error(SeckillCodeMsg.PAY_STATUS_CHANGE);
    }

    @Override
    public int changePayStatus(String orderNo, Integer statusAccountPaid, Integer paytypeOnline) {
        return orderInfoMapper.changePayStatus(orderNo,statusAccountPaid, paytypeOnline);
    }

    @Override
    public void refundOnline(OrderInfo orderInfo) {
        RefundVo vo = new RefundVo();
        vo.setOutTradeNo(orderInfo.getOrderNo());
        vo.setRefundAmount(String.valueOf(orderInfo.getSeckillPrice()));
        vo.setRefundReason("不想要了");
        Result<Boolean> result = payFeignApi.refund(vo);
        if (result == null || result.hasError() || !result.getData()) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);
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

    @Override
    @GlobalTransactional
    public void payIntegral(String orderNo) {
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        if (OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            PayLog log = new PayLog();
            log.setOrderNo(orderNo);
            log.setPayTime(new Date());
            log.setTotalAmount(String.valueOf(orderInfo.getSeckillPrice()));
            log.setPayType(OrderInfo.PAYTYPE_INTERGRAL);
            payLogMapper.insert(log);
            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(orderInfo.getUserId());
            vo.setValue(orderInfo.getIntergral());
            Result result = integralFeignApi.decrIntegral(vo);
            if (result == null || result.hasError()) {
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }
            int effectCount = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_INTERGRAL);
            if (effectCount == 0) {
                throw new BusinessException(SeckillCodeMsg.PAY_ERROR);
            }
//            int o = 1/0;
        }
    }

    @Override
    @GlobalTransactional
//    @Transactional
    public void refundIntegral(OrderInfo orderInfo) {
        if (OrderInfo.STATUS_ACCOUNT_PAID.equals(orderInfo.getStatus())) {
            RefundLog log = new RefundLog();
            log.setOrderNo(orderInfo.getOrderNo());
            log.setRefundReason("不要了");
            log.setRefundAmount(orderInfo.getIntergral());
            log.setRefundTime(new Date());
            log.setRefundType(OrderInfo.PAYTYPE_INTERGRAL);
            refundLogMapper.insert(log);

            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(orderInfo.getUserId());
            vo.setValue(orderInfo.getIntergral());
            Result result = integralFeignApi.incrIntegral(vo);
            if (result == null || result.hasError()) {
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }

            int effectCount = orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);
            if (effectCount == 0) {
                throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
            }
//            int a = 1/0;
//            try {
//                TimeUnit.SECONDS.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }
}