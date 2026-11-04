package br.com.szella.orderprocessor;

import br.com.szella.orderprocessor.domain.model.OrderStatus;
import br.com.szella.orderprocessor.domain.model.OrderType;
import br.com.szella.orderprocessor.infrastructure.adapters.in.broker.OrderCreatedEvent;
import br.com.szella.orderprocessor.infrastructure.adapters.out.database.OrderEntity;
import br.com.szella.orderprocessor.infrastructure.adapters.out.database.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class OrderProcessorIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        orderJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve consumir evento do Kafka e atualizar o status da ordem no Banco para PROCESSED")
    void shouldConsumeEventAndProcessOrder() {
        // 1. Preparação (Arrange): Inserir uma ordem PENDING diretamente no banco de dados
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderEntity pendingOrder = new OrderEntity(
                orderId, customerId, "WEGE3", 100, new BigDecimal("40.00"),
                OrderType.BUY, OrderStatus.PENDING, LocalDateTime.now()
        );
        orderJpaRepository.save(pendingOrder);

        // 2. Ação (Act): Simular a Order API publicando o evento de criação no Kafka
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, customerId, "WEGE3", 100, new BigDecimal("40.00"),
                "BUY", "PENDING", LocalDateTime.now()
        );
        kafkaTemplate.send("order-created-events", orderId.toString(), event);

        // 3. Verificação Assíncrona (Assert via Awaitility)
        // Aguarda até 10 segundos para que o Consumer leia o Kafka e atualize o Postgres
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    OrderEntity updatedOrder = orderJpaRepository.findById(orderId).orElseThrow();
                    assertEquals(OrderStatus.PROCESSED, updatedOrder.getStatus());
                });
    }
}
