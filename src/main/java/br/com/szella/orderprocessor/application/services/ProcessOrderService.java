package br.com.szella.orderprocessor.application.services;

import br.com.szella.orderprocessor.domain.model.OrderStatus;
import br.com.szella.orderprocessor.infrastructure.adapters.out.database.OrderEntity;
import br.com.szella.orderprocessor.infrastructure.adapters.out.database.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessOrderService {

    private final OrderJpaRepository orderJpaRepository;

    @Transactional
    public void execute(UUID orderId) {
        OrderEntity order = orderJpaRepository.findById(orderId)
                .orElseThrow(()->new IllegalArgumentException("Ordem não encontrada: " + orderId));

        if(order.getStatus() != OrderStatus.PENDING){
            log.warn("Mensagem duplicada ignorada (Idempotência). Ordem {} já está com status {}", orderId, order.getStatus());
            return;
        }

        log.info("Processando ordem {} para o ativo {}", order.getId(), order.getTicker());

        order.setStatus(OrderStatus.PROCESSED);
        orderJpaRepository.save(order);

        log.info("Ordem {} processada com sucesso!", order.getId());
    }
}
