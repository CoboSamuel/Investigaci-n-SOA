
package com.bikestore.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Exchanges
    public static final String ORDERS_EXCHANGE =
            "bikestore.orders.exchange";

    public static final String PAYMENTS_EXCHANGE =
            "bikestore.payments.exchange";

    public static final String DLX_EXCHANGE =
            "bikestore.dlx.exchange";

    // Colas
    public static final String PAYMENT_QUEUE =
            "bikestore.payments.queue";

    public static final String EMAIL_QUEUE =
            "bikestore.email.queue";

    public static final String DLQ =
            "bikestore.dlq";

    // Claves de enrutamiento
    public static final String ORDER_CREATED =
            "order.created";

    public static final String PAYMENT_PAID =
            "payment.paid";

    public static final String PAYMENT_FAILED =
            "payment.failed";

    @Bean
    public Declarables rabbitTopology() {

        // Exchanges que distribuyen los mensajes
        DirectExchange ordersExchange =
                new DirectExchange(ORDERS_EXCHANGE);

        DirectExchange paymentsExchange =
                new DirectExchange(PAYMENTS_EXCHANGE);

        DirectExchange deadLetterExchange =
                new DirectExchange(DLX_EXCHANGE);

        // Cola principal para procesar pagos
        Queue paymentQueue = QueueBuilder
                .durable(PAYMENT_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(PAYMENT_FAILED)
                .build();

        // Cola para las confirmaciones
        Queue emailQueue = QueueBuilder
                .durable(EMAIL_QUEUE)
                .build();

        // Cola para los pagos fallidos
        Queue deadLetterQueue = QueueBuilder
                .durable(DLQ)
                .build();

        return new Declarables(
                ordersExchange,
                paymentsExchange,
                deadLetterExchange,

                paymentQueue,
                emailQueue,
                deadLetterQueue,

                BindingBuilder.bind(paymentQueue)
                        .to(ordersExchange)
                        .with(ORDER_CREATED),

                BindingBuilder.bind(emailQueue)
                        .to(paymentsExchange)
                        .with(PAYMENT_PAID),

                BindingBuilder.bind(deadLetterQueue)
                        .to(deadLetterExchange)
                        .with(PAYMENT_FAILED)
        );
    }

    @Bean
    public ApplicationRunner comprobarRabbitMQ(
            AmqpAdmin admin) {

        return args -> {
            if (admin.getQueueInfo(PAYMENT_QUEUE) == null
                    || admin.getQueueInfo(EMAIL_QUEUE) == null
                    || admin.getQueueInfo(DLQ) == null) {
                throw new IllegalStateException(
                        "No se pudieron verificar las colas");
            }

            System.out.println(
                    "[BikeStore] Conexion con RabbitMQ correcta");

            System.out.println(
                    "[BikeStore] Tres colas verificadas");
        };
    }
}
