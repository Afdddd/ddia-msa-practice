package org.core.orderservice;

import org.core.proto.product.GetProductRequest;
import org.core.proto.product.GetProductResponse;
import org.core.proto.product.ProductServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class ProductGrpcClient {

    private final ProductServiceGrpc.ProductServiceBlockingStub productStub;

    public ProductGrpcClient(ProductServiceGrpc.ProductServiceBlockingStub productStub) {
        this.productStub = productStub;
    }

    public GetProductResponse getProduct(Long productId) {
        return productStub.getProduct(GetProductRequest.newBuilder()
                .setProductId(productId)
                .build());
    }
}
