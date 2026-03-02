package com.ynov.tp1.repository;

import com.ynov.tp1.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProductRepository {

    private static final Duration DEFAULT_LATENCY   = Duration.ofMillis(100);
    private static final double   DEFAULT_ERROR_RATE = 0.10;

    private final Random   random;
    private final Duration latency;
    private final double   errorRate;

    private static final Map<String, Product> PRODUCTS = Map.of(
        "PROD001", new Product("PROD001", "Laptop Pro",    new BigDecimal("999.99"),  10, "Electronique"),
        "PROD002", new Product("PROD002", "Smartphone X",  new BigDecimal("599.99"),  25, "Electronique"),
        "PROD003", new Product("PROD003", "Livre Java",    new BigDecimal("39.99"),    5, "Livres"),
        "PROD004", new Product("PROD004", "Casque Audio",  new BigDecimal("149.99"),   0, "Accessoires"),
        "PROD005", new Product("PROD005", "Chaise Bureau", new BigDecimal("299.99"),   8, "Mobilier")
    );

    public ProductRepository() {
        this(DEFAULT_LATENCY, DEFAULT_ERROR_RATE);
    }

    public ProductRepository(boolean randomErrorsEnabled) {
        this(DEFAULT_LATENCY, randomErrorsEnabled ? DEFAULT_ERROR_RATE : 0.0);
    }

    public ProductRepository(Duration latency, double errorRate) {
        this.latency   = latency;
        this.errorRate = errorRate;
        this.random    = new Random();
    }

    public Mono<Product> findById(String id) {
        return Mono.defer(() -> {
                if (errorRate > 0 && random.nextDouble() < errorRate) {
                    return Mono.error(new RuntimeException("Erreur DB aléatoire pour le produit : " + id));
                }
                Product product = PRODUCTS.get(id);
                return product != null ? Mono.just(product) : Mono.empty();
            })
            .delayElement(latency);
    }

    public Flux<Product> findByIds(List<String> ids) {
        return Flux.just(ids)
            .flatMapIterable(list -> list)
            .flatMap(this::findById);
    }

    public Mono<Integer> getStock(String productId) {
        return Mono.defer(() -> {
                if (errorRate > 0 && random.nextDouble() < errorRate) {
                    return Mono.error(new RuntimeException("Erreur DB stock pour le produit : " + productId));
                }
                Product product = PRODUCTS.get(productId);
                return product != null ? Mono.just(product.getStock()) : Mono.just(0);
            })
            .delayElement(latency);
    }
}
