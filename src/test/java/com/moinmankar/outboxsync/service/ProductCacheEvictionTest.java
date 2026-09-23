package com.moinmankar.outboxsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moinmankar.outboxsync.dto.request.CreateOrderRequest;
import com.moinmankar.outboxsync.dto.response.ProductResponse;
import com.moinmankar.outboxsync.entity.Product;
import com.moinmankar.outboxsync.entity.User;
import com.moinmankar.outboxsync.repository.OrderRepository;
import com.moinmankar.outboxsync.repository.OutboxEventRepository;
import com.moinmankar.outboxsync.repository.ProductRepository;
import com.moinmankar.outboxsync.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ProductCacheEvictionTest.TestConfig.class)
class ProductCacheEvictionTest {

    @Autowired
    ProductService productService;

    @Autowired
    OrderService orderService;

    @Autowired
    CacheManager cacheManager;

    @MockitoBean
    ProductRepository productRepository;

    @MockitoBean
    OrderRepository orderRepository;

    @MockitoBean
    OutboxEventRepository outboxEventRepository;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    ActivityLogService activityLogService;

    private final UUID productId = UUID.randomUUID();

    private Product newProduct(int stock) {
        Product product = new Product();
        product.setId(productId);
        product.setName("Widget");
        product.setDescription("desc");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(stock);
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    @Test
    void cachedProductIsEvictedAfterOrderDecrementsStock() {

        Product product = newProduct(10);
        User user = new User();
        user.setId(UUID.randomUUID());

        AtomicInteger repoCalls = new AtomicInteger();

        when(productRepository.findById(productId)).thenAnswer(invocation -> {
            repoCalls.incrementAndGet();
            return Optional.of(product);
        });
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse first = productService.getProductById(productId);
        assertThat(first.stockQuantity()).isEqualTo(10);
        assertThat(repoCalls.get()).isEqualTo(1);

        ProductResponse cached = productService.getProductById(productId);
        assertThat(cached.stockQuantity()).isEqualTo(10);
        assertThat(repoCalls.get())
                .as("second read is served from cache")
                .isEqualTo(1);

        orderService.createOrder("user@example.com", new CreateOrderRequest(productId, 2));

        assertThat(cacheManager.getCache("products").get(productId)).isNull();

        int callsBeforeReRead = repoCalls.get();
        ProductResponse afterOrder = productService.getProductById(productId);
        assertThat(afterOrder.stockQuantity())
                .as("cache evicted so fresh reduced stock is loaded")
                .isEqualTo(8);
        assertThat(repoCalls.get())
                .as("post-eviction read hits the repository again")
                .isEqualTo(callsBeforeReRead + 1);
    }

    @Configuration
    @EnableCaching
    @ComponentScan(
            basePackageClasses = {ProductService.class, OrderService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {ProductService.class, OrderService.class}
            )
    )
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("products");
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
