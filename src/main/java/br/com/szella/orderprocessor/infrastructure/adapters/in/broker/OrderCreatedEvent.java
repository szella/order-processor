package br.com.szella.orderprocessor.infrastructure.adapters.in.broker;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID id,
        UUID customerId,
        String ticker,
        Integer quantity,
        BigDecimal price,
        String type,
        String status,
        LocalDateTime createdAt
) {
}
