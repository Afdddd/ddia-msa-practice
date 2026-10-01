package org.core.orderservice;

import org.core.proto.product.GetProductRequest;
import org.core.proto.product.GetProductResponse;
import org.core.proto.product.ProductServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ProductServiceGrpc.ProductServiceBlockingStub productStub;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderService(ProductServiceGrpc.ProductServiceBlockingStub productStub,
                        KafkaTemplate<String, String> kafkaTemplate) {
        this.productStub = productStub;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Long order(OrderRequest request) {
        GetProductResponse product = productStub.getProduct(toRequest(request.getProductId()));
        kafkaTemplate.send("order-created", "productId = " + request.getProductId() + ", quantity = "+request.getQuantity())
                .whenComplete((result, ex) -> {
                    if(ex != null) {
                        log.error("send failed", ex);
                        return;
                    }
                    log.info("sent: partition={}, offset={}",
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                            );
                });

        return 0L;
    }

    private GetProductRequest toRequest(Long productId) {
        return GetProductRequest.newBuilder()
                .setProductId(productId)
                .build();
    }
}
