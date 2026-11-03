
package com.bikestore.model;

import java.math.BigDecimal;

public record OrderMessage(
        String pedidoId,
        String cliente,
        String correo,
        String producto,
        BigDecimal precio,
        String paymentStatus
) {
}
