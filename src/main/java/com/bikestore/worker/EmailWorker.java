
package com.bikestore.worker;

import com.bikestore.config.RabbitConfig;
import com.bikestore.model.OrderMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class EmailWorker {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailWorker.class);

    private final JsonMapper jsonMapper;

    public EmailWorker(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void procesarEmail(Message message) {

        OrderMessage pedido;

        try {
            String json = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            pedido = jsonMapper.readValue(
                    json,
                    OrderMessage.class
            );

            if (pedido == null) {
                throw new IllegalArgumentException(
                        "Mensaje de correo vacio"
                );
            }

        } catch (JacksonException | IllegalArgumentException ex) {

            logger.error(
                    "pedidoId=DESCONOCIDO timestamp={} thread={} evento=EMAIL_INVALID_MESSAGE",
                    Instant.now(),
                    Thread.currentThread().getName()
            );

            throw new AmqpRejectAndDontRequeueException(
                    "Mensaje de correo invalido", ex
            );
        }

        registrar(pedido.pedidoId(), "EMAIL_RECEIVED");

        // Solo confirmar pedidos pagados
        if (!"PAID".equals(pedido.paymentStatus())) {

            registrar(
                    pedido.pedidoId(),
                    "EMAIL_SKIPPED_NOT_PAID"
            );

            return;
        }

        // Simulacion del envio del correo
        logger.info(
                "pedidoId={} timestamp={} thread={} evento=EMAIL_SENT_SIMULATED destinatario={}",
                pedido.pedidoId(),
                Instant.now(),
                Thread.currentThread().getName(),
                pedido.correo()
        );
    }

    private void registrar(String pedidoId, String evento) {

        logger.info(
                "pedidoId={} timestamp={} thread={} evento={}",
                pedidoId,
                Instant.now(),
                Thread.currentThread().getName(),
                evento
        );
    }
}
