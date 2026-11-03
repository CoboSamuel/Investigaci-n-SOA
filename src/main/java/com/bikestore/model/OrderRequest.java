
package com.bikestore.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(

        @NotBlank(message = "El cliente es obligatorio")
        String cliente,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        String correo,

        @NotBlank(message = "El producto es obligatorio")
        String producto,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01",
                message = "El precio debe ser mayor que cero")
        BigDecimal precio
) {
}
