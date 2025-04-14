package cn.wolfcode.web.feign.impl;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.ProductFeignApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductFeignFallback implements ProductFeignApi {
    @Override
    public Result<List<Product>> queryByIds(List<Long> productIds) {
//        System.out.println("调用了备用功能");
        return null;
    }
}