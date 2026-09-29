package org.core.orderservice;

import org.core.proto.product.ProductServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(target = "product", types = ProductServiceGrpc.ProductServiceBlockingStub.class)
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
