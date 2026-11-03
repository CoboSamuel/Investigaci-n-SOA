
package com.bikestore.worker;

import com.bikestore.config.RabbitConfig;
import com.bikestore.model.OrderMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PaymentWorker {

    private static final Logger logger =
            LoggerFactory.getLogger(PaymentWorker.class);

    private static final int MAX_REINTENTOS = 3;

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;
    private final String modoPrueba;

    public PaymentWorker(
            RabbitTemplate rabbitTemplate,
            JsonMapper jsonMapper,
            @Value("${bikestore.payment.mode:random}")
            String modoPrueba) {

        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
        this.modoPrueba = modoPrueba;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void procesarPago(Message message) {

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

        } catch (JacksonException ex) {
            throw new AmqpRejectAndDontRequeueException(
                    "El JSON del pedido no es valido", ex
            );
        }

        registrar(pedido.pedidoId(), "PAYMENT_RECEIVED", 0);

        int maxIntentos = MAX_REINTENTOS + 1;

        for (int intento = 1; intento <= maxIntentos; intento++) {

            registrar(
                    pedido.pedidoId(),
                    "PAYMENT_ATTEMPT",
                    intento
            );

            if (simularPago()) {

                OrderMessage pedidoPagado =
                        new OrderMessage(
                                pedido.pedidoId(),
                                pedido.cliente(),
                                pedido.correo(),
                                pedido.producto(),
                                pedido.precio(),
                                "PAID"
                        );

                String jsonPagado =
                        jsonMapper.writeValueAsString(pedidoPagado);

                rabbitTemplate.convertAndSend(
                        RabbitConfig.PAYMENTS_EXCHANGE,
                        RabbitConfig.PAYMENT_PAID,
                        jsonPagado,
                        outgoing -> {
                            outgoing.getMessageProperties()
                                    .setContentType(
                                            MessageProperties.CONTENT_TYPE_JSON
                                    );
                            return outgoing;
                        }
                );

                registrar(
                        pedido.pedidoId(),
                        "PAYMENT_PAID",
                        intento
                );

                return;
            }

            registrar(
                    pedido.pedidoId(),
                    "PAYMENT_FAILED",
                    intento
            );

            if (intento < maxIntentos) {
                registrar(
                        pedido.pedidoId(),
                        "PAYMENT_RETRY",
                        intento + 1
                );
            }
        }

        registrar(
                pedido.pedidoId(),
                "PAYMENT_REJECTED_FOR_DLQ",
                maxIntentos
        );

        throw new AmqpRejectAndDontRequeueException(
                "Pago fallido tras agotar los reintentos: "
                        + pedido.pedidoId()
        );
    }

    private boolean simularPago() {

        // Modos especiales para las pruebas del deber
        if ("success".equalsIgnoreCase(modoPrueba)) {
            return true;
        }

        if ("failure".equalsIgnoreCase(modoPrueba)) {
            return false;
        }

        // Modo normal: 50 % de probabilidad de exito
        return ThreadLocalRandom.current().nextBoolean();
    }

    private void registrar(
            String pedidoId,
            String evento,
            int intento) {

        logger.info(
                "pedidoId={} timestamp={} thread={} evento={} intento={}",
                pedidoId,
                Instant.now(),
                Thread.currentThread().getName(),
                evento,
                intento
        );
    }
}
