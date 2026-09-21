package com.moinmankar.outboxsync.controller;


import com.moinmankar.outboxsync.dto.request.CreateProductRequest;
import com.moinmankar.outboxsync.dto.response.ProductResponse;
import com.moinmankar.outboxsync.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
