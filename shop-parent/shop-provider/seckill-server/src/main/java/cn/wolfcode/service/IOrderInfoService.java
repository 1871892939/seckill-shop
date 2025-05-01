package cn.wolfcode.service;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.Map;

/**
 * Created by wolfcode-lanxw
 */
public interface IOrderInfoService {

    OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId);

    OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo);

    OrderInfo findByOrderNo(String orderNo);

    void cancelOrder(String orderNo);

    Result<String> payOnline(String orderNo);

    int changePayStatus(String orderNo, Integer statusAccountPaid, Integer paytypeOnline);

    void refundOnline(OrderInfo orderInfo);

    void payIntegral(String orderNo);

    void refundIntegral(OrderInfo orderInfo);
}
