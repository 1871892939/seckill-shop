package cn.wolfcode.web.controller;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.feign.PayFeignApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/orderPay")
@RefreshScope
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Qualifier("cn.wolfcode.web.feign.PayFeignApi")
    @Autowired
    private PayFeignApi payFeignApi;

    @RequestMapping("/pay")
    public Result<String> pay(String orderNo, Integer type) {
        if (OrderInfo.PAYTYPE_ONLINE.equals(type)) {
            return orderInfoService.payOnline(orderNo);
        } else {
            orderInfoService.payIntegral(orderNo);
            return Result.success();
        }
    }

    @RequestMapping("/notifyUrl")
    public String notifyUrl(@RequestParam Map<String, String> params) {
        System.out.println("异步回调");
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError()) {
            return "fail";
        }
        boolean signVerified = result.getData();
        if(signVerified) {
            String orderNo = params.get("out_trade_no");

            int effectCount = orderInfoService.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_ONLINE);
            if (effectCount == 0) {

            }
        }
        return "success";
    }

    @Value("${pay.errorUrl}")
    private String errorUrl;
    @Value("${pay.frontEndPayUrl}")
    private String frontEndPayUrl;
    @RequestMapping("/returnUrl")
    public void returnUrl(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        System.out.println("同步回调");
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError() || !result.getData()) {
            response.sendRedirect(errorUrl);
            return;
        }
        String orderNo = params.get("out_trade_no");
        response.sendRedirect(frontEndPayUrl+orderNo);
    }


    @RequestMapping("/refund")
    public Result<String> refund(String orderNo) {
        OrderInfo orderInfo = orderInfoService.findByOrderNo(orderNo);
        if (OrderInfo.PAYTYPE_ONLINE.equals(orderInfo.getPayType())) {
            orderInfoService.refundOnline(orderInfo);
        } else {
            orderInfoService.refundIntegral(orderInfo);
        }

        return Result.success();
    }
}