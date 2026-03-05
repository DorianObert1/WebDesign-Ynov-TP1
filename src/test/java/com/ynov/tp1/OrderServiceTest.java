package com.ynov.tp1;

import com.ynov.tp1.exception.InvalidOrderException;
import com.ynov.tp1.model.Order;
import com.ynov.tp1.model.OrderRequest;
import com.ynov.tp1.model.OrderStatus;
import com.ynov.tp1.repository.ProductRepository;
import com.ynov.tp1.service.OrderService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceTest.class);

    private final ProductRepository repository = new ProductRepository(false);
    private final OrderService orderService = new OrderService(repository);

    @Test
    void test_processOrderSuccess() {
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD002"),
                "CUST001"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isNotNull();
                    assertThat(order.getProducts()).hasSize(2);
                    assertThat(order.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getDiscountApplied()).isTrue();
                    assertThat(order.getCreatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithInvalidIds() {
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "INVALID_ID", "PROD003"),
                "CUST002"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getProducts()).hasSize(2);
                    assertThat(order.getProducts().size()).isLessThan(3);
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithoutStock() {
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD004", "PROD001"),
                "CUST003"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getProducts()).hasSize(1);
                    assertThat(order.getProducts().get(0).getProduct().getId()).isEqualTo("PROD001");
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithDiscounts() {
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD003"),
                "CUST004"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getProducts()).hasSize(2);

                    order.getProducts().forEach(pwp -> {
                        String category = pwp.getProduct().getCategory();
                        if ("Electronique".equalsIgnoreCase(category)) {
                            assertThat(pwp.getDiscountPercentage()).isEqualTo(10);
                        } else {
                            assertThat(pwp.getDiscountPercentage()).isEqualTo(5);
                        }
                        BigDecimal expected = pwp.getOriginalPrice()
                                .multiply(BigDecimal.valueOf(100 - pwp.getDiscountPercentage()))
                                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                        assertThat(pwp.getFinalPrice()).isEqualByComparingTo(expected);
                    });

                    BigDecimal expectedTotal = order.getProducts().stream()
                            .map(pwp -> pwp.getFinalPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    assertThat(order.getTotalPrice()).isEqualByComparingTo(expectedTotal);
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderTimeout() {
        ProductRepository slowRepository = new ProductRepository(Duration.ofSeconds(6), 0.0);
        OrderService slowService = new OrderService(slowRepository);

        OrderRequest request = new OrderRequest(List.of("PROD001"), "CUST005");

        StepVerifier.withVirtualTime(() -> slowService.processOrder(request))
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(6))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
                    assertThat(order.getProducts()).isEmpty();
                    assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithErrors() {
        ProductRepository errorRepository = new ProductRepository(Duration.ofMillis(10), 0.5);
        OrderService errorService = new OrderService(errorRepository);

        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD002", "PROD003", "PROD005"),
                "CUST006"
        );

        StepVerifier.create(errorService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order).isNotNull();
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getProducts().size()).isGreaterThanOrEqualTo(0);
                    assertThat(order.getTotalPrice()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderParallel_correctness() {
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD002", "PROD003", "PROD005"),
                "CUST007"
        );

        StepVerifier.create(orderService.processOrderParallel(request))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getProducts()).hasSize(4);
                    assertThat(order.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderParallel_fasterThanSequential() {
        ProductRepository slowRepo = new ProductRepository(Duration.ofMillis(200), 0.0);
        OrderService slowService = new OrderService(slowRepo);

        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD002", "PROD003", "PROD005"),
                "CUST008"
        );

        long startSeq = System.currentTimeMillis();
        slowService.processOrder(request).block();
        long durationSeq = System.currentTimeMillis() - startSeq;

        long startPar = System.currentTimeMillis();
        slowService.processOrderParallel(request).block();
        long durationPar = System.currentTimeMillis() - startPar;

        log.info("Séquentiel : {}ms | Parallèle : {}ms | Gain : {}ms",
                durationSeq, durationPar, durationSeq - durationPar);

        assertThat(durationPar).isLessThan(durationSeq);
    }

    @Test
    void test_processOrderInvalidRequest() {
        OrderRequest request = new OrderRequest(List.of("PROD001"), null);

        StepVerifier.create(orderService.processOrder(request))
                .expectError(InvalidOrderException.class)
                .verify();
    }
}
