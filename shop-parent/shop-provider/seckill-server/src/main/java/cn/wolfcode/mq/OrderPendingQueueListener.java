package cn.wolfcode.mq;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "penddingGroup", topic = MQConstant.ORDER_PEDDING_TOPIC)
public class OrderPendingQueueListener implements RocketMQListener<OrderMessage> {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(OrderMessage orderMessage) {
        OrderMQResult result = new OrderMQResult();
        result.setToken(orderMessage.getToken());
        String tag;

        try {
            SeckillProductVo vo = seckillProductService.findFromCache(orderMessage.getTime(), orderMessage.getSeckillId());
            OrderInfo orderInfo = orderInfoService.doSeckill(String.valueOf(orderMessage.getUserPhone()), vo);
            result.setOrderNo(orderInfo.getOrderNo());
            tag = MQConstant.ORDER_RESULT_SUCCESS_TAG;
        } catch (Exception e) {
            e.printStackTrace();
            result.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            result.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            result.setTime(orderMessage.getTime());
            result.setSeckillId(orderMessage.getSeckillId());
            tag = MQConstant.ORDER_RESULT_FAIL_TAG;
        }
        rocketMQTemplate.syncSend(MQConstant.ORDER_RESULT_TOPIC+":"+tag, result);
    }
}
