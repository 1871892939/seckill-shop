package cn.wolfcode.service;


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
}
