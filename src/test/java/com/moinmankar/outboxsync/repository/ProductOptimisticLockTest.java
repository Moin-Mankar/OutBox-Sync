package com.moinmankar.outboxsync.repository;

import com.moinmankar.outboxsync.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductOptimisticLockTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    EntityManagerFactory emf;

    @Test
    void secondConcurrentWriterOnStaleVersionIsDetected() {

        UUID productId = seed(10);
        try {
            EntityManager loser = emf.createEntityManager();
            loser.getTransaction().begin();
            Product loserCopy = loser.find(Product.class, productId);
            assertThat(loserCopy.getVersion()).isNotNull();
            assertThat(loserCopy.getStockQuantity()).isEqualTo(10);

            EntityManager winner = emf.createEntityManager();
            winner.getTransaction().begin();
            Product winnerCopy = winner.find(Product.class, productId);
            winnerCopy.setStockQuantity(winnerCopy.getStockQuantity() - 1);
            winner.flush();
            winner.getTransaction().commit();
            winner.close();

            loserCopy.setStockQuantity(loserCopy.getStockQuantity() - 1);

            assertThatThrownBy(() -> {
                loser.flush();
                loser.getTransaction().commit();
            }).isInstanceOf(OptimisticLockException.class);

            if (loser.getTransaction().isActive()) {
                loser.getTransaction().rollback();
            }
            loser.close();

            Product finalState = reload(productId);
            assertThat(finalState.getStockQuantity()).isEqualTo(9);
        } finally {
            delete(productId);
        }
    }

    @Test
    void normalStockUpdateStillWorksAndIncrementsVersion() {

        UUID productId = seed(5);
        try {
            Product product = reload(productId);
            assertThat(product.getVersion()).isNotNull();
            long before = product.getVersion();

            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            Product managed = em.find(Product.class, productId);
            managed.setStockQuantity(3);
            em.flush();
            em.getTransaction().commit();
            em.close();

            Product updated = reload(productId);
            assertThat(updated.getStockQuantity()).isEqualTo(3);
            assertThat(updated.getVersion()).isGreaterThan(before);
        } finally {
            delete(productId);
        }
    }

    private UUID seed(int stock) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Product product = new Product();
        product.setName("test-product");
        product.setDescription("test");
        product.setPrice(new BigDecimal("9.99"));
        product.setStockQuantity(stock);
        product.setActive(true);
        em.persist(product);
        em.getTransaction().commit();
        UUID id = product.getId();
        em.close();
        return id;
    }

    private Product reload(UUID id) {
        EntityManager em = emf.createEntityManager();
        Product product = em.find(Product.class, id);
        em.close();
        return product;
    }

    private void delete(UUID id) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Product product = em.find(Product.class, id);
        if (product != null) {
            em.remove(product);
        }
        em.getTransaction().commit();
        em.close();
    }
}
