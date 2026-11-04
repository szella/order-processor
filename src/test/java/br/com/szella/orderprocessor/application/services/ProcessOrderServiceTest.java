package br.com.szella.orderprocessor.application.services;

import br.com.szella.orderprocessor.domain.model.OrderStatus;
import br.com.szella.orderprocessor.domain.model.OrderType;
import br.com.szella.orderprocessor.infrastructure.adapters.out.database.OrderEntity;
import br.com.szella.orderprocessor.infrastructure.adapters.out.database.OrderJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessOrderServiceTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @InjectMocks
    private ProcessOrderService processOrderService;

    @Test
    @DisplayName("Deve processar a ordem e atualizar o status para PROCESSED")
    void shouldProcessPendingOrder() {
        UUID orderId = UUID.randomUUID();
        OrderEntity pendingOrder = new OrderEntity(
                orderId, UUID.randomUUID(), "PETR4", 100, new BigDecimal("35.00"),
                OrderType.BUY, OrderStatus.PENDING, LocalDateTime.now()
        );

        when(orderJpaRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        processOrderService.execute(orderId);

        assertEquals(OrderStatus.PROCESSED, pendingOrder.getStatus());
        verify(orderJpaRepository, times(1)).save(pendingOrder);
    }

    @Test
    @DisplayName("Deve ignorar mensagem duplicada (Idempotência) se ordem já estiver PROCESSED")
    void shouldIgnoreAlreadyProcessedOrder() {
        UUID orderId = UUID.randomUUID();
        OrderEntity processedOrder = new OrderEntity(
                orderId, UUID.randomUUID(), "VALE3", 50, new BigDecimal("60.00"),
                OrderType.SELL, OrderStatus.PROCESSED, LocalDateTime.now()
        );

        when(orderJpaRepository.findById(orderId)).thenReturn(Optional.of(processedOrder));

        processOrderService.execute(orderId);

        // Garante que o repositório NÃO foi chamado para salvar novamente
        verify(orderJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção se a ordem não for encontrada")
    void shouldThrowExceptionWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderJpaRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> processOrderService.execute(orderId));
    }
}