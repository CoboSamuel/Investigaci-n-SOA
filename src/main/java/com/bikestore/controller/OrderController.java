
package com.bikestore.controller;

import com.bikestore.model.OrderMessage;
import com.bikestore.model.OrderRequest;
import com.bikestore.producer.OrderProducer;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger =
            LoggerFactory.getLogger(OrderController.class);

    private final OrderProducer orderProducer;

    public OrderController(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> crearPedido(
            @Valid @RequestBody OrderRequest request) {

        OrderMessage pedido =
                orderProducer.publicarPedido(request);

        logger.info(
                "pedidoId={} timestamp={} thread={} evento=ORDER_RECEIVED",
                pedido.pedidoId(),
                Instant.now(),
                Thread.currentThread().getName()
        );

        return ResponseEntity.accepted().body(
                Map.of(
                        "pedidoId", pedido.pedidoId(),
                        "paymentStatus", pedido.paymentStatus(),
                        "mensaje", "Pedido recibido para procesamiento"
                )
        );
    }
}
