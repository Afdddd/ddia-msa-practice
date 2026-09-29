package org.core.orderservice;

import org.core.proto.product.GetProductRequest;
import org.core.proto.product.GetProductResponse;
import org.core.proto.product.ProductServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ProductServiceGrpc.ProductServiceBlockingStub productStub;

    public OrderService(ProductServiceGrpc.ProductServiceBlockingStub productStub) {
        this.productStub = productStub;
    }

    public Long order(OrderRequest request) {
        GetProductResponse product = productStub.getProduct(toRequest(request.getProductId()));
        log.info("product name: {}", product.getName());
        return 0L; // 주문 저장 전 임시값. orders 테이블 추가 후 생성된 주문 id 반환
    }

    private GetProductRequest toRequest(Long productId) {
        return GetProductRequest.newBuilder()
                .setProductId(productId)
                .build();
    }
}
