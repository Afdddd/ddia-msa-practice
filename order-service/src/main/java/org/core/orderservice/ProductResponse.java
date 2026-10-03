package org.core.orderservice;

public record ProductResponse(Long id, String name, long price, int stock) {
}
