package org.core.productservice;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.core.proto.product.GetProductRequest;
import org.core.proto.product.GetProductResponse;
import org.core.proto.product.ProductServiceGrpc;
import org.springframework.stereotype.Service;

@Service
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductRepository repository;

    public ProductGrpcService(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<GetProductResponse> responseObserver) {
        Product product = repository.findById(request.getProductId()).orElse(null);

        if (product == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("product not found: " + request.getProductId())
                            .asRuntimeException());
            return;
        }

        GetProductResponse response = GetProductResponse.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .setPrice(product.getPrice())
                .setStock(product.getStock())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
