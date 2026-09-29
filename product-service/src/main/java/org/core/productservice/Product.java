package org.core.productservice;

import jakarta.persistence.*;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private long price;
    @Column(nullable = false)
    private int stock;

    protected Product() {}
    public Product(
            String name,
            long price,
            int stock
    ) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public Long getId() { return this.id; }
    public String getName() { return this.name; }
    public long getPrice() { return this.price; }
    public int getStock() { return this.stock; }
}
