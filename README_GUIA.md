# BikeStore Async v1.0

**Autor:** Samuel Cobo  
**Materia:** Diseño y Arquitectura de Software  
**Proyecto:** Investigación SOA

## Descripción

BikeStore Async es una aplicación en Java y Spring Boot que simula el procesamiento asíncrono de pedidos de bicicletas utilizando RabbitMQ.

El proyecto permite recibir pedidos mediante una API REST, procesar pagos con una probabilidad de fallo del 50 %, realizar tres reintentos y enviar los mensajes fallidos a una Dead-Letter Queue.

También cuenta con EmailWorker, que simula el envío de confirmaciones únicamente cuando el pago fue aprobado.

Este README explica cómo ejecutar la aplicación y comprobar cada funcionalidad.

## 1. Requisitos

Para ejecutar el proyecto se necesita:

- Java 21
- Docker Desktop
- PowerShell o terminal de VS Code
- Conexión a Internet para la primera descarga de dependencias

## 2. Preparar el proyecto

Clonar el repositorio, si todavía no está descargado:

```powershell
git clone https://github.com/CoboSamuel/Investigaci-n-SOA.git
cd Investigaci-n-SOA
```

Crear el archivo de configuración local:

```powershell
Copy-Item .env.example .env
```

Abrir `.env` y colocar las credenciales de desarrollo:

```properties
RABBITMQ_USER=bikestore
RABBITMQ_PASSWORD=dev12345
```

Estas credenciales son únicamente de prueba. El archivo `.env` no se sube a GitHub.

Si se utiliza una contraseña diferente, también se debe configurar `RABBITMQ_PASSWORD` en el entorno donde se ejecuta Spring Boot.

## 3. Iniciar RabbitMQ

Primero abrir Docker Desktop.

En la terminal del proyecto ejecutar:

```powershell
docker compose up -d
```

Comprobar que RabbitMQ está funcionando:

```powershell
docker compose ps
```

El contenedor `bikestore-rabbitmq` debe aparecer en estado `Up`.

Después abrir en el navegador:

http://localhost:15672

Ingresar con el usuario y la contraseña configurados en `.env`.

En la pestaña **Queues and Streams** deben aparecer las siguientes colas:

| Cola | Función |
|---|---|
| `bikestore.payments.queue` | Pedidos pendientes de pago |
| `bikestore.email.queue` | Confirmaciones de pagos aprobados |
| `bikestore.dlq` | Pedidos que fallaron después de los reintentos |

Las colas se crean automáticamente al iniciar Spring Boot.

## 4. Ejecutar la aplicación Java

En la terminal de VS Code ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Si funciona, en la consola aparecerán mensajes similares a estos:

```text
Started BikestoreAsyncApplication
[BikeStore] Conexion con RabbitMQ correcta
[BikeStore] Tres colas verificadas
```

La API queda disponible en:

http://localhost:8080

Mantener esta terminal abierta mientras se realizan las pruebas. Para enviar pedidos, abrir una segunda terminal.

---

## 5. Prueba 1: OrderProducer publica un JSON

**Objetivo:** comprobar que la API reciba una compra y publique un mensaje en RabbitMQ.

Con la aplicación ejecutándose, abrir otra terminal de PowerShell y escribir:

```powershell
$body = @{
    cliente = "Samuel"
    correo = "samuel@example.com"
    producto = "Bicicleta Trek Marlin"
    precio = 850.00
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/orders" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

**Resultado esperado:**

La API devuelve una respuesta similar a:

```text
mensaje        : Pedido recibido para procesamiento
pedidoId       : PED-EF5488E4
paymentStatus  : PENDING
```

El identificador del pedido será diferente en cada ejecución.

En la consola de Spring Boot deben aparecer los eventos:

```text
ORDER_PUBLISHED
ORDER_RECEIVED
```

Para comprobar el paso por RabbitMQ, abrir:

http://localhost:15672

Entrar a **Queues and Streams**.

Si PaymentWorker está activo, puede consumir el pedido inmediatamente y dejar la cola de pagos vacía. En ese caso, los registros de `PAYMENT_RECEIVED` permiten confirmar que el consumidor recibió el mensaje publicado.

**La prueba es correcta cuando la API responde con un pedido y los registros muestran su publicación y recepción.**

---

## 6. Prueba 2: PaymentWorker procesa pagos con 50 % de fallo

**Objetivo:** comprobar la simulación de pagos.

Abrir:

`src/main/resources/application.properties`

Verificar que esté configurado:

```properties
bikestore.payment.mode=random
```

Este es el modo normal de funcionamiento.

Cada intento de pago tiene un 50 % de probabilidad de aprobarse y un 50 % de fallar.

Ejecutar nuevamente la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

En otra terminal, enviar uno o varios pedidos utilizando el comando de la Prueba 1.

**Resultado esperado:**

En los registros de PaymentWorker aparecen eventos como:

```text
PAYMENT_RECEIVED
PAYMENT_ATTEMPT
PAYMENT_FAILED
PAYMENT_RETRY
PAYMENT_PAID
```

Los eventos exactos dependen del resultado aleatorio.

Cuando un pago es aprobado, PaymentWorker publica un nuevo mensaje JSON con el estado `PAID`.

Este evento se envía a `bikestore.email.queue`.

**La prueba es correcta cuando PaymentWorker consume el pedido y registra el resultado del pago.**

---

## 7. Prueba 3: Tres reintentos y Dead-Letter Queue

**Objetivo:** comprobar qué ocurre cuando un pago falla repetidamente.

Para realizar esta prueba sin depender del azar, se utiliza el modo `failure`.

Primero detener Spring Boot con `Ctrl + C`.

En PowerShell ejecutar:

```powershell
$env:BIKESTORE_PAYMENT_MODE = "failure"
.\mvnw.cmd spring-boot:run
```

Con la aplicación funcionando, abrir otra terminal y enviar un nuevo pedido utilizando el comando de la Prueba 1.

**Resultado esperado:**

PaymentWorker registra los intentos fallidos:

```text
PAYMENT_ATTEMPT intento=1
PAYMENT_FAILED intento=1

PAYMENT_RETRY intento=2
PAYMENT_ATTEMPT intento=2
PAYMENT_FAILED intento=2

PAYMENT_RETRY intento=3
PAYMENT_ATTEMPT intento=3
PAYMENT_FAILED intento=3

PAYMENT_RETRY intento=4
PAYMENT_ATTEMPT intento=4
PAYMENT_FAILED intento=4

PAYMENT_REJECTED_FOR_DLQ intento=4
```

En esta implementación se realiza un intento inicial y tres reintentos adicionales.

Después del último fallo, PaymentWorker rechaza el mensaje y RabbitMQ lo envía a la Dead-Letter Queue.

Para comprobarlo, entrar a:

http://localhost:15672

Seleccionar **Queues and Streams** y revisar:

`bikestore.dlq`

Debe aparecer un nuevo mensaje en la columna `Ready`.

Si ya existían mensajes fallidos, la cantidad aumentará en uno.

También se puede hacer clic en la cola para revisar el mensaje almacenado. No es necesario eliminarlo.

**La prueba es correcta cuando aparecen los cuatro intentos fallidos en consola y el mensaje llega a `bikestore.dlq`.**

Al terminar, detener Spring Boot y eliminar la variable temporal:

```powershell
Remove-Item Env:\BIKESTORE_PAYMENT_MODE -ErrorAction SilentlyContinue
```

---

## 8. Prueba 4: EmailWorker confirma un pago exitoso

**Objetivo:** comprobar que EmailWorker procese los pedidos pagados.

Primero detener Spring Boot si está ejecutándose.

Para forzar un pago exitoso, ejecutar:

```powershell
$env:BIKESTORE_PAYMENT_MODE = "success"
.\mvnw.cmd spring-boot:run
```

Abrir otra terminal y enviar un pedido utilizando el comando de la Prueba 1.

**Resultado esperado:**

Para un mismo `pedidoId`, deben aparecer los eventos:

```text
ORDER_PUBLISHED
ORDER_RECEIVED
PAYMENT_RECEIVED
PAYMENT_ATTEMPT
PAYMENT_PAID
EMAIL_RECEIVED
EMAIL_SENT_SIMULATED
```

Esto demuestra que el pedido pasó por OrderProducer, PaymentWorker y EmailWorker.

En RabbitMQ, las colas de pagos y correos deberían quedar sin mensajes pendientes después del procesamiento.

**La prueba es correcta cuando aparece `EMAIL_SENT_SIMULATED` para el pedido aprobado.**

El correo es simulado mediante un registro en consola. No se envía un correo real.

Al finalizar, detener Spring Boot y ejecutar:

```powershell
Remove-Item Env:\BIKESTORE_PAYMENT_MODE -ErrorAction SilentlyContinue
```

---

## 9. Prueba 5: EmailWorker no confirma pedidos PENDING

**Objetivo:** comprobar que no se genere una confirmación si el pedido no está pagado.

Ejecutar Spring Boot normalmente:

```powershell
.\mvnw.cmd spring-boot:run
```

Después abrir el panel:

http://localhost:15672

Realizar los siguientes pasos:

1. Entrar a **Exchanges**.
2. Seleccionar `(AMQP default)`.
3. Buscar la sección **Publish message**.
4. En `Routing key` escribir `bikestore.email.queue`.
5. En `Payload` colocar el siguiente JSON:

```json
{
  "pedidoId": "PED-PRUEBA-PENDING",
  "cliente": "Samuel",
  "correo": "samuel@example.com",
  "producto": "Bicicleta de montaña",
  "precio": 650.00,
  "paymentStatus": "PENDING"
}
```

6. Presionar **Publish message**.

**Resultado esperado:**

En la consola de Spring Boot deben aparecer estos eventos para el mismo pedido:

```text
pedidoId=PED-PRUEBA-PENDING evento=EMAIL_RECEIVED

pedidoId=PED-PRUEBA-PENDING evento=EMAIL_SKIPPED_NOT_PAID
```

No debe aparecer `EMAIL_SENT_SIMULATED` para `PED-PRUEBA-PENDING`.

**La prueba es correcta cuando EmailWorker recibe el mensaje, detecta que no está pagado y omite la confirmación.**

---

## 10. Prueba 6: Comprobar los registros de ejecución

**Objetivo:** verificar que los eventos incluyan `pedidoId`, `timestamp` y `thread`.

Al realizar cualquiera de las pruebas anteriores, revisar la terminal de Spring Boot.

Los registros tienen una estructura similar a:

```text
pedidoId=PED-5DE6C52A
timestamp=2026-11-03T22:22:37.372986Z
thread=org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#0-1
evento=PAYMENT_REJECTED_FOR_DLQ
intento=4
```

Cada registro permite identificar:

- `pedidoId`: pedido que se está procesando.
- `timestamp`: fecha y hora del evento.
- `thread`: hilo encargado de ejecutar la operación.
- `evento`: acción realizada.
- `intento`: número del intento de pago, cuando corresponde.

**La prueba es correcta cuando estos datos aparecen en los registros de pedidos, pagos y confirmaciones.**

---

## 11. Prueba 7: Comprobar que el proyecto es ejecutable

**Objetivo:** verificar que el proyecto compile y pase las pruebas configuradas.

Detener Spring Boot y ejecutar:

```powershell
.\mvnw.cmd clean verify
```

**Resultado esperado:**

```text
BUILD SUCCESS
```

Maven compila las clases, ejecuta las pruebas automatizadas disponibles y genera el archivo JAR dentro de la carpeta `target`.

Para ejecutar el JAR generado:

```powershell
java -jar target/bikestore-async-0.0.1-SNAPSHOT.jar
```

RabbitMQ debe estar encendido y configurado correctamente.

La compilación exitosa no sustituye las pruebas funcionales anteriores; ambas comprobaciones son necesarias.

---

## 12. Evidencias

Las capturas realizadas durante el desarrollo se encuentran en:

`docs/evidencias/`

Incluyen las comprobaciones de:

- Publicación de pedidos.
- Procesamiento de pagos.
- Reintentos y Dead-Letter Queue.
- Confirmaciones de EmailWorker.
- Rechazo de pedidos sin pago aprobado.
- Compilación con `BUILD SUCCESS`.

Los reportes de investigación SOA y comparación de gestores de mensajería se encuentran en:

`docs/reportes/`

## 13. Finalizar la ejecución

Para detener Spring Boot:

`Ctrl + C`

Para detener RabbitMQ:

```powershell
docker compose down
```

Los mensajes almacenados en el volumen de RabbitMQ se conservan al utilizar `docker compose down` sin la opción `-v`.

## Conclusión

Mediante estas pruebas se comprobó el funcionamiento de BikeStore Async v1.0.

La aplicación permite recibir pedidos mediante una API REST, publicarlos en RabbitMQ, procesar pagos con reintentos y almacenar los mensajes fallidos en una Dead-Letter Queue.

También se comprobó que EmailWorker solamente simula confirmaciones cuando el pago está aprobado y que los registros permiten seguir cada pedido durante su procesamiento.