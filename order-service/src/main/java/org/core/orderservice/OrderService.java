package org.core.orderservice;

import org.core.proto.product.GetProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ProductGrpcClient productGrpcClient;
    private final ProductRestClient productRestClient;

    public OrderService(ProductGrpcClient productGrpcClient, ProductRestClient productRestClient) {
        this.productGrpcClient = productGrpcClient;
        this.productRestClient = productRestClient;
    }

    public ProductResponse getProduct(Long productId) {
        GetProductResponse grpcProduct = productGrpcClient.getProduct(productId);
        log.info("[gRPC] {} bytes, {}", grpcProduct.getSerializedSize(), grpcProduct);

        ProductResponse restProduct = productRestClient.getProduct(productId);
        log.info("[REST] {}", restProduct);

        return restProduct;
    }
}
