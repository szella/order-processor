package br.com.szella.orderprocessor.infrastructure.adapters.in.broker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqConsumerAdapter {

    @KafkaListener(topics = "order-created-events.DLT", groupId = "order-processor-dlq-group")
    public void consumeDlq(OrderCreatedEvent event) {
        log.error("ALERT: Mensagem enviada para a DLQ após falhas de retentativa! Ordem ID: {}", event.id());
    }
}
