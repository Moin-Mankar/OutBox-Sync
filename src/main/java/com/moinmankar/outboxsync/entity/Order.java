package com.moinmankar.outboxsync.entity;

import com.moinmankar.outboxsync.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 12 , scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status= OrderStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id" , nullable = false)
    private Product product;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate(){
        this.createdAt= LocalDateTime.now();
        this.updatedAt= LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate(){
        this.updatedAt= LocalDateTime.now();
    }
}
