package br.com.szella.orderprocessor.infrastructure.adapters.in.broker;

import br.com.szella.orderprocessor.application.services.ProcessOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumerAdapter {

    private final ProcessOrderService processOrderService;

    @KafkaListener(topics = "order-created-events", groupId = "order-processor-group")
    public void consume(OrderCreatedEvent event) {
        log.info("Evento recebido do Kafka para a Ordem ID: {}", event.id());

        processOrderService.execute(event.id());
    }
}
