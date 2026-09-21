package com.moinmankar.outboxsync.repository;

import com.moinmankar.outboxsync.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}