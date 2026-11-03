
package com.bikestore.producer;

import com.bikestore.config.RabbitConfig;
import com.bikestore.model.OrderMessage;
import com.bikestore.model.OrderRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(OrderProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public OrderProducer(
            RabbitTemplate rabbitTemplate,
            JsonMapper jsonMapper) {

        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
    }

    public OrderMessage publicarPedido(OrderRequest request) {

        String pedidoId = "PED-" +
                UUID.randomUUID().toString()
                        .substring(0, 8)
                        .toUpperCase();

        OrderMessage pedido = new OrderMessage(
                pedidoId,
                request.cliente(),
                request.correo(),
                request.producto(),
                request.precio(),
                "PENDING"
        );

        // Convertir el pedido Java en JSON
        String json = jsonMapper.writeValueAsString(pedido);

        // Publicar el JSON en RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDER_CREATED,
                json,
                message -> {
                    message.getMessageProperties()
                            .setContentType(
                                    MessageProperties.CONTENT_TYPE_JSON
                            );
                    return message;
                }
        );

        logger.info(
                "pedidoId={} timestamp={} thread={} evento=ORDER_PUBLISHED",
                pedidoId,
                Instant.now(),
                Thread.currentThread().getName()
        );

        return pedido;
    }
}
