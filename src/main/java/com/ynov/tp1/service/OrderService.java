package com.ynov.tp1.service;

import com.ynov.tp1.exception.InvalidOrderException;
import com.ynov.tp1.model.Order;
import com.ynov.tp1.model.OrderRequest;
import com.ynov.tp1.model.OrderStatus;
import com.ynov.tp1.model.ProductWithPrice;
import com.ynov.tp1.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final int      DISCOUNT_ELECTRONICS = 10;
    private static final int      DISCOUNT_OTHER       = 5;
    private static final int      MAX_PRODUCTS         = 100;
    private static final Duration TIMEOUT              = Duration.ofSeconds(5);

    private final ProductRepository productRepository;

    public OrderService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Mono<Order> processOrder(OrderRequest request) {
        if (request == null
                || request.getCustomerId() == null
                || request.getProductIds() == null
                || request.getProductIds().isEmpty()) {
            return Mono.error(new InvalidOrderException(
                    "La requête est invalide : customerId et productIds sont obligatoires"));
        }

        return Mono.just(request)
            .doOnNext(r -> log.info("Démarrage du traitement de la commande pour le client {}", r.getCustomerId()))
            .flatMap(r ->
                Flux.fromIterable(r.getProductIds())
                    .filter(id -> id != null && !id.isBlank())
                    .take(MAX_PRODUCTS)
                    .doOnNext(id -> log.debug("Traitement de l'ID produit : {}", id))
                    .flatMap(id ->
                        productRepository.findById(id)
                            .doOnNext(p -> log.debug("Produit trouvé : {}", p))
                            .onErrorResume(e -> {
                                log.warn("Erreur lors de la récupération du produit {} : {}", id, e.getMessage());
                                return Mono.empty();
                            })
                    )
                    .filter(p -> {
                        boolean inStock = p.getStock() > 0;
                        if (!inStock) log.debug("Produit {} hors stock, ignoré", p.getId());
                        return inStock;
                    })
                    .map(this::applyDiscount)
                    .collectList()
                    .map(products -> buildOrder(r.getProductIds(), products))
                    .doOnNext(order -> log.info("Commande créée : {} — {} produit(s) — total={}",
                            order.getOrderId(), order.getProducts().size(), order.getTotalPrice()))
            )
            .timeout(TIMEOUT)
            .doOnError(e -> log.error("Erreur lors du traitement de la commande : {}", e.getMessage()))
            .onErrorResume(e -> {
                log.warn("Création d'une commande en échec suite à l'erreur : {}", e.getMessage());
                return Mono.just(new Order(request.getProductIds(), Collections.emptyList(),
                        BigDecimal.ZERO, false, OrderStatus.FAILED));
            })
            .doFinally(signal -> log.info("Fin du traitement de la commande (signal: {})", signal));
    }

    public Mono<Order> processOrderParallel(OrderRequest request) {
        if (request == null
                || request.getCustomerId() == null
                || request.getProductIds() == null
                || request.getProductIds().isEmpty()) {
            return Mono.error(new InvalidOrderException(
                    "La requête est invalide : customerId et productIds sont obligatoires"));
        }

        return Mono.just(request)
            .doOnNext(r -> log.info("[PARALLEL] Démarrage pour le client {}", r.getCustomerId()))
            .flatMap(r ->
                Flux.fromIterable(r.getProductIds())
                    .filter(id -> id != null && !id.isBlank())
                    .take(MAX_PRODUCTS)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .flatMap(id ->
                        productRepository.findById(id)
                            .doOnNext(p -> log.debug("[PARALLEL] Produit trouvé : {}", p))
                            .onErrorResume(e -> {
                                log.warn("[PARALLEL] Erreur produit {} : {}", id, e.getMessage());
                                return Mono.empty();
                            })
                    )
                    .filter(p -> p.getStock() > 0)
                    .map(this::applyDiscount)
                    .sequential()
                    .collectList()
                    .map(products -> buildOrder(r.getProductIds(), products))
                    .doOnNext(order -> log.info("[PARALLEL] Commande créée : {} — {} produit(s) — total={}",
                            order.getOrderId(), order.getProducts().size(), order.getTotalPrice()))
            )
            .timeout(TIMEOUT)
            .doOnError(e -> log.error("[PARALLEL] Erreur : {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(new Order(request.getProductIds(), Collections.emptyList(),
                    BigDecimal.ZERO, false, OrderStatus.FAILED)))
            .doFinally(signal -> log.info("[PARALLEL] Fin du traitement (signal: {})", signal));
    }

    private ProductWithPrice applyDiscount(com.ynov.tp1.model.Product p) {
        int discountPct = "Electronique".equalsIgnoreCase(p.getCategory())
                ? DISCOUNT_ELECTRONICS
                : DISCOUNT_OTHER;
        BigDecimal discount = p.getPrice()
                .multiply(BigDecimal.valueOf(discountPct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new ProductWithPrice(p, p.getPrice(), discountPct, p.getPrice().subtract(discount));
    }

    private Order buildOrder(List<String> productIds, List<ProductWithPrice> products) {
        BigDecimal totalPrice = products.stream()
                .map(ProductWithPrice::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(productIds, products, totalPrice, !products.isEmpty(), OrderStatus.COMPLETED);
    }
}
