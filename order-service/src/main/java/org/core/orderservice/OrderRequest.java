package org.core.orderservice;

public class OrderRequest {
    private Long productId;
    private int quantity;

    public Long getProductId() { return this.productId; }
    public int getQuantity() { return this.quantity; }
}
