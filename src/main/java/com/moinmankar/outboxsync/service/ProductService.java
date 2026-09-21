package com.moinmankar.outboxsync.service;


import com.moinmankar.outboxsync.dto.request.CreateProductRequest;
import com.moinmankar.outboxsync.dto.response.ProductResponse;
import com.moinmankar.outboxsync.entity.Product;
import com.moinmankar.outboxsync.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(CreateProductRequest request) {

        Product product = new Product();

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(true);

        Product savedProduct = productRepository.save(product);

        return new ProductResponse(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getDescription(),
                savedProduct.getPrice(),
                savedProduct.getStockQuantity(),
                savedProduct.isActive(),
                savedProduct.getCreatedAt(),
                savedProduct.getUpdatedAt()
        );
    }
}
