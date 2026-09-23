package com.moinmankar.outboxsync.controller;

import com.moinmankar.outboxsync.dto.response.ProductResponse;
import com.moinmankar.outboxsync.exception.BusinessException;
import com.moinmankar.outboxsync.exception.ResourceNotFoundException;
import com.moinmankar.outboxsync.repository.ProductRepository;
import com.moinmankar.outboxsync.security.CustomUserDetailsService;
import com.moinmankar.outboxsync.security.JwtAuthenticationFilter;
import com.moinmankar.outboxsync.security.JwtService;
import com.moinmankar.outboxsync.security.SecurityConfig;
import com.moinmankar.outboxsync.service.OrderService;
import com.moinmankar.outboxsync.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@WebMvcTest(controllers = {ProductController.class, OrderController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductWebLayerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService productService;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    ProductRepository productRepository;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    @MockitoBean
    PasswordEncoder passwordEncoder;

    @MockitoBean
    JwtService jwtService;

    private static final String VALID_PRODUCT =
            "{\"name\":\"Widget\",\"description\":\"d\",\"price\":9.99,\"stockQuantity\":5}";

    private final UUID id = UUID.randomUUID();

    private ProductResponse sample() {
        return new ProductResponse(
                id, "Widget", "d", new BigDecimal("9.99"), 5,
                true, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateProduct() throws Exception {
        when(productService.createProduct(any())).thenReturn(sample());
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotCreateProduct() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdateProduct() throws Exception {
        when(productService.updateProduct(any(), any())).thenReturn(sample());
        mvc.perform(put("/api/v1/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotUpdateProduct() throws Exception {
        mvc.perform(put("/api/v1/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteProduct() throws Exception {
        mvc.perform(delete("/api/v1/products/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotDeleteProduct() throws Exception {
        mvc.perform(delete("/api/v1/products/" + id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanReadProduct() throws Exception {
        when(productService.getProductById(any())).thenReturn(sample());
        mvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void productNotFoundReturns404() throws Exception {
        when(productService.getProductById(any()))
                .thenThrow(new ResourceNotFoundException("Product not found"));
        mvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/products/" + id));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validationFailureReturns400() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"d\",\"price\":null,\"stockQuantity\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void optimisticLockReturns409() throws Exception {
        when(productService.updateProduct(any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(
                        com.moinmankar.outboxsync.entity.Product.class, id));
        mvc.perform(put("/api/v1/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unexpectedErrorReturns500AndHidesDetails() throws Exception {
        when(productService.updateProduct(any(), any()))
                .thenThrow(new RuntimeException("secret internal db detail"));
        mvc.perform(put("/api/v1/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message").value(not(containsString("secret internal db detail"))));
    }

    @Test
    @WithMockUser(roles = "USER")
    void businessExceptionReturns400() throws Exception {
        when(orderService.createOrder(anyString(), any()))
                .thenThrow(new BusinessException("Insufficient stock"));
        mvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + id + "\",\"quantity\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient stock"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unsupportedMethodReturns405() throws Exception {
        mvc.perform(put("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.path").value("/api/v1/products"));
    }
}
