package cn.wolfcode.service.impl;

import cn.wolfcode.domain.Product;
import cn.wolfcode.mapper.ProductMapper;
import cn.wolfcode.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by lanxw
 */
@Service
public class ProductServiceImpl implements IProductService {
    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Product> queryByIds(List<Long> productIds) {
        if (productIds == null || productIds.size() == 0) {
//            System.out.println("id为空或0");
            return Collections.EMPTY_LIST;
        }
//        System.out.println("调用了查id");
        return productMapper.queryProductByIds(productIds);
    }
}
