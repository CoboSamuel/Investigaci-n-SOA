# BikeStore Async v1.0

**Autor:** Samuel Cobo  
**Materia:** Diseño y Arquitectura de Software  
**Proyecto:** Aplicación asíncrona con Spring Boot y RabbitMQ

## Descripción

BikeStore Async v1.0 es una aplicación que simula la compra de bicicletas mediante una página web conectada a una API desarrollada con Java y Spring Boot.

Cuando un cliente realiza una compra, el pedido se envía a RabbitMQ para procesarlo de manera asíncrona.

El proyecto cuenta con dos consumidores:

- **PaymentWorker:** simula pagos con un 50 % de probabilidad de fallo por intento y permite hasta tres reintentos adicionales.
- **EmailWorker:** simula una confirmación por correo únicamente cuando el estado del pago es `PAID`.

Si se agotan los intentos de pago, el mensaje se envía a una **Dead-Letter Queue (DLQ)**.

Este README explica cómo ejecutar el proyecto y comprobar cada funcionalidad solicitada en la tarea.

---

## 1. Tecnologías utilizadas

| Tecnología | Función |
|---|---|
| Java 21 | Desarrollo del backend |
| Spring Boot 4.1.2 | API REST y procesamiento de pedidos |
| RabbitMQ | Comunicación asíncrona entre componentes |
| Docker | Ejecución del servidor RabbitMQ |
| Maven | Compilación y dependencias |
| HTML, CSS y JavaScript | Página web de BikeStore |
| Git y GitHub | Control de versiones |

## 2. Funcionamiento del sistema

El procesamiento de una compra sigue este recorrido:

```text
                  PÁGINA WEB
                      |
               Cliente compra
                      |
                      v
              POST /orders
                      |
                      v
                OrderProducer
                      |
                 Mensaje JSON
                      |
                      v
                    RabbitMQ
                      |
                      v
             payments.queue
                      |
                      v
                PaymentWorker
                      |
             Simulación de pago
                      |
            +---------+---------+
            |                   |
          PAID                FALLIDO
            |                   |
            v              Hasta 3 reintentos
       email.queue              |
            |              Si todos fallan
            v                   |
        EmailWorker              v
            |                   DLQ
            v
    Confirmación simulada
```

La API responde inicialmente con el estado `PENDING`, ya que el procesamiento del pago se realiza después de recibir la solicitud.

### Colas utilizadas

| Cola | Función |
|---|---|
| `bikestore.payments.queue` | Recibe los pedidos pendientes de pago |
| `bikestore.email.queue` | Recibe eventos de pagos aprobados |
| `bikestore.dlq` | Almacena mensajes rechazados después de agotar los reintentos |

---

# GUÍA PARA COMPROBAR EL FUNCIONAMIENTO

## 3. Requisitos para ejecutar el proyecto

Antes de comenzar, verificar que estén instalados:

- Java JDK 21.
- Docker Desktop.
- Visual Studio Code o una terminal de PowerShell.
- Git para clonar el repositorio.

### Descargar el proyecto

Si todavía no está descargado, ejecutar:

```powershell
git clone https://github.com/CoboSamuel/Investigaci-n-SOA.git

cd Investigaci-n-SOA
```

## 4. Iniciar RabbitMQ

### Paso 1. Configurar las credenciales

Desde la carpeta principal del proyecto, ejecutar:

```powershell
Copy-Item .env.example .env
```

Abrir el archivo `.env` y configurar las credenciales locales:

```properties
RABBITMQ_USER=bikestore
RABBITMQ_PASSWORD=dev12345
```

Estos valores son únicamente para el entorno de demostración.

Si se utiliza una contraseña distinta, también se debe establecer `RABBITMQ_PASSWORD` en la terminal desde la cual se inicia Spring Boot.

### Paso 2. Encender Docker

Abrir **Docker Desktop** y comprobar que el motor esté funcionando.

Después, en la terminal de VS Code, ejecutar:

```powershell
docker compose up -d
```

Comprobar que el contenedor esté activo:

```powershell
docker compose ps
```

**Resultado esperado:**

El contenedor `bikestore-rabbitmq` debe aparecer en estado `Up`.

### Paso 3. Abrir RabbitMQ

Ingresar desde el navegador:

http://localhost:15672

Iniciar sesión con las credenciales configuradas en `.env`.

Las tres colas del proyecto aparecerán cuando Spring Boot termine de iniciarse.

## 5. Iniciar la aplicación Spring Boot

En la terminal principal del proyecto ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

**Resultado esperado:**

En los registros de la terminal deben aparecer mensajes similares a:

```text
Started BikestoreAsyncApplication

[BikeStore] Conexion con RabbitMQ correcta

[BikeStore] Tres colas verificadas
```

Esto confirma que la aplicación inició y se conectó correctamente a RabbitMQ.

Después se puede abrir:

http://localhost:8080

En esa dirección debe aparecer la página web de BikeStore.

---

## 6. PRUEBA 1 — Verificar la página web y OrderProducer

**Objetivo:** comprobar que un cliente pueda seleccionar una bicicleta y enviar un pedido a la API.

### Cómo realizar la prueba

1. Tener Docker Desktop funcionando.
2. Tener Spring Boot ejecutándose.
3. Abrir http://localhost:8080.
4. Seleccionar una bicicleta del catálogo.
5. Presionar el botón **Comprar**.
6. Ingresar nombre y correo electrónico.
7. Presionar **Confirmar compra**.

### Resultado esperado

La página debe mostrar:

```text
¡Pedido recibido!

Pedido recibido para procesamiento

ID del pedido: PED-XXXXXXXX
Estado: PENDING
```

El identificador cambia en cada compra.

### Resultado que obtuvimos

Durante nuestras pruebas, la página generó el pedido:

```text
PED-F7475D46
```

Y mostró:

```text
Estado: PENDING
```

Esto confirma que el frontend se comunicó con la API `POST /orders`.

### Comprobar en la consola

En la terminal de Spring Boot deben aparecer:

```text
evento=ORDER_PUBLISHED
evento=ORDER_RECEIVED
```

Ambos eventos deben corresponder al mismo `pedidoId`.

**Prueba correcta:** la página permite comprar una bicicleta, genera un identificador y OrderProducer publica el pedido.

---

## 7. PRUEBA 2 — Enviar un pedido directamente a la API

**Objetivo:** comprobar que la API también funciona sin utilizar la página web.

Esta prueba se realiza desde PowerShell.

Con Spring Boot ejecutándose, abrir una segunda terminal y escribir:

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

### Resultado esperado

```text
mensaje       : Pedido recibido para procesamiento
pedidoId      : PED-XXXXXXXX
paymentStatus : PENDING
```

El pedido se convierte en JSON y se publica en RabbitMQ.

### Comprobar en RabbitMQ

Abrir:

http://localhost:15672

Ingresar a **Queues and Streams**.

La cola `bikestore.payments.queue` recibe el mensaje publicado.

Si PaymentWorker ya está activo, puede consumirlo inmediatamente. Por ese motivo, es normal que la cola muestre cero mensajes pendientes después del procesamiento.

En ese caso, revisar el evento `PAYMENT_RECEIVED` en la terminal.

**Prueba correcta:** la API responde con un `pedidoId` y el consumidor recibe el mensaje publicado.

---

## 8. PRUEBA 3 — PaymentWorker y probabilidad de fallo del 50 %

**Objetivo:** comprobar que PaymentWorker procese los pagos y simule fallos aleatorios.

### Preparar la prueba

Detener Spring Boot con `Ctrl + C`.

En PowerShell eliminar cualquier modo de prueba anterior:

```powershell
Remove-Item Env:\BIKESTORE_PAYMENT_MODE -ErrorAction SilentlyContinue
```

Iniciar nuevamente la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación utiliza el modo `random`, configurado en:

`src/main/resources/application.properties`

```properties
bikestore.payment.mode=random
```

### Cómo realizar la prueba

1. Abrir http://localhost:8080.
2. Realizar varias compras desde el catálogo.
3. Revisar los registros de PaymentWorker en la terminal.

### Resultado esperado

Se pueden observar eventos como:

```text
evento=PAYMENT_RECEIVED
evento=PAYMENT_ATTEMPT intento=1
evento=PAYMENT_FAILED intento=1
evento=PAYMENT_RETRY intento=2
```

Si el pago es aprobado, aparece:

```text
evento=PAYMENT_PAID
```

La simulación tiene un 50 % de probabilidad de fallo **en cada intento**.

Por eso, el resultado puede ser diferente en cada ejecución.

**Prueba correcta:** PaymentWorker recibe pedidos y registra intentos de pago con resultados aleatorios.

---

## 9. PRUEBA 4 — Tres reintentos y Dead-Letter Queue

**Objetivo:** comprobar que los pedidos rechazados terminen en la DLQ.

Para esta prueba utilizamos el modo `failure`, que permite provocar errores sin depender del azar.

### Paso 1. Activar el modo de fallo

Detener Spring Boot con `Ctrl + C`.

En PowerShell ejecutar:

```powershell
$env:BIKESTORE_PAYMENT_MODE = "failure"

.\mvnw.cmd spring-boot:run
```

### Paso 2. Realizar un pedido

Abrir:

http://localhost:8080

Seleccionar una bicicleta y confirmar la compra.

### Paso 3. Revisar los reintentos

En la terminal de Spring Boot aparecerán los siguientes eventos:

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

El sistema realiza un intento inicial y tres reintentos adicionales.

Después de agotar los intentos, rechaza el mensaje para que RabbitMQ lo envíe a la Dead-Letter Queue.

Es normal que Spring registre una excepción relacionada con el rechazo del mensaje durante esta prueba.

### Paso 4. Comprobar RabbitMQ

Abrir:

http://localhost:15672

Entrar a **Queues and Streams** y revisar:

`bikestore.dlq`

**Resultado esperado:**

Debe aparecer un nuevo mensaje en la DLQ.

Si antes estaba vacía, mostrará:

```text
Ready: 1
```

Si ya tenía mensajes de pruebas anteriores, la cantidad aumentará en uno.

La cola de pagos debería quedar vacía después de que se procese el mensaje.

### Resultado que obtuvimos

Durante nuestras pruebas, el pedido:

```text
PED-5DE6C52A
```

Falló en los cuatro intentos y fue enviado correctamente a `bikestore.dlq`.

**Prueba correcta:** se observan los reintentos en consola y el mensaje rechazado aparece en la DLQ.

### Volver al modo normal

Detener Spring Boot y ejecutar:

```powershell
Remove-Item Env:\BIKESTORE_PAYMENT_MODE -ErrorAction SilentlyContinue
```

---

## 10. PRUEBA 5 — EmailWorker confirma pagos exitosos

**Objetivo:** comprobar que un pago aprobado genere un evento de confirmación.

### Paso 1. Activar pagos exitosos

Detener Spring Boot.

En PowerShell ejecutar:

```powershell
$env:BIKESTORE_PAYMENT_MODE = "success"

.\mvnw.cmd spring-boot:run
```

Este modo se utiliza solamente para probar el procesamiento exitoso.

### Paso 2. Comprar desde la página web

1. Abrir http://localhost:8080.
2. Seleccionar una bicicleta.
3. Ingresar los datos del cliente.
4. Confirmar la compra.

### Paso 3. Revisar la consola

Para el mismo `pedidoId` deben aparecer los siguientes eventos:

```text
ORDER_PUBLISHED
ORDER_RECEIVED

PAYMENT_RECEIVED
PAYMENT_ATTEMPT
PAYMENT_PAID

EMAIL_RECEIVED
EMAIL_SENT_SIMULATED
```

### Resultado que obtuvimos

En la compra realizada desde la página web se generó:

```text
PED-F7475D46
```

En la consola se verificaron:

```text
pedidoId=PED-F7475D46 evento=PAYMENT_RECEIVED

pedidoId=PED-F7475D46 evento=PAYMENT_ATTEMPT intento=1

pedidoId=PED-F7475D46 evento=PAYMENT_PAID intento=1

pedidoId=PED-F7475D46 evento=EMAIL_RECEIVED

pedidoId=PED-F7475D46 evento=EMAIL_SENT_SIMULATED
```

El pago fue aprobado y EmailWorker registró una confirmación simulada.

### Comprobar RabbitMQ

Ingresar a:

http://localhost:15672

Revisar que, después de completar el procesamiento, las colas:

- `bikestore.payments.queue`
- `bikestore.email.queue`

No tengan mensajes pendientes de esa compra.

**Prueba correcta:** un pedido realizado desde la página web llega a PaymentWorker y después a EmailWorker.

**Nota:** el correo no se envía realmente. La aplicación simula su envío mediante registros en consola.

### Finalizar el modo de prueba

Detener Spring Boot y ejecutar:

```powershell
Remove-Item Env:\BIKESTORE_PAYMENT_MODE -ErrorAction SilentlyContinue
```

---

## 11. PRUEBA 6 — EmailWorker no confirma pedidos PENDING

**Objetivo:** comprobar que no se genere una confirmación cuando el pago no está aprobado.

### Paso 1. Ejecutar Spring Boot

Iniciar la aplicación normalmente:

```powershell
.\mvnw.cmd spring-boot:run
```

### Paso 2. Publicar un mensaje de prueba

Abrir RabbitMQ:

http://localhost:15672

1. Entrar a **Exchanges**.
2. Seleccionar `(AMQP default)`.
3. Buscar **Publish message**.
4. En `Routing key` escribir:

```text
bikestore.email.queue
```

5. En `Payload` colocar este JSON:

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

### Paso 3. Revisar la consola

**Resultado esperado:**

```text
pedidoId=PED-PRUEBA-PENDING evento=EMAIL_RECEIVED

pedidoId=PED-PRUEBA-PENDING evento=EMAIL_SKIPPED_NOT_PAID
```

Para ese pedido no debe aparecer:

```text
EMAIL_SENT_SIMULATED
```

### Resultado que obtuvimos

Durante nuestras pruebas, EmailWorker recibió el pedido `PED-PRUEBA-PENDING` y omitió su confirmación.

**Prueba correcta:** EmailWorker identifica que el estado no es `PAID` y no simula el envío.

---

## 12. PRUEBA 7 — Registros con pedidoId, timestamp y thread

**Objetivo:** comprobar que los registros incluyan los datos solicitados en la rúbrica.

Durante cualquier prueba, revisar la terminal donde está ejecutándose Spring Boot.

Un registro de la aplicación tiene una estructura similar a:

```text
pedidoId=PED-F7475D46
timestamp=2026-11-04T05:03:38.426609700Z
thread=org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#1-1
evento=PAYMENT_PAID
intento=1
```

### Datos registrados

| Campo | Descripción |
|---|---|
| `pedidoId` | Identificador del pedido |
| `timestamp` | Fecha y hora del evento |
| `thread` | Hilo que procesó el mensaje |
| `evento` | Operación realizada |
| `intento` | Número de intento, cuando corresponde |

Los eventos se registran desde OrderProducer, OrderController, PaymentWorker y EmailWorker.

**Prueba correcta:** los registros permiten identificar el pedido, el momento en que se procesó y el hilo responsable.

---

## 13. PRUEBA 8 — Compilación del proyecto

**Objetivo:** comprobar que el proyecto pueda compilarse correctamente.

Primero detener Spring Boot con `Ctrl + C`.

Después ejecutar:

```powershell
.\mvnw.cmd clean verify
```

### Resultado esperado

Al finalizar debe aparecer:

```text
BUILD SUCCESS
```

Este comando compila la aplicación, ejecuta las pruebas automatizadas configuradas y genera el archivo JAR.

Durante nuestras pruebas, Maven terminó correctamente y generó el archivo:

```text
target/bikestore-async-0.0.1-SNAPSHOT.jar
```

También es posible ejecutar ese archivo mediante:

```powershell
java -jar target/bikestore-async-0.0.1-SNAPSHOT.jar
```

Para ello, RabbitMQ debe estar funcionando.

**Prueba correcta:** Maven finaliza con `BUILD SUCCESS`.

---

## 14. Evidencias de funcionamiento

Las capturas de pantalla obtenidas durante las pruebas se guardan en:

`docs/evidencias/`

Entre las evidencias se encuentran:

- Configuración y conexión de RabbitMQ.
- Publicación de pedidos desde la API.
- Procesamiento de pagos.
- Reintentos y Dead-Letter Queue.
- Confirmación de pagos exitosos.
- Rechazo de confirmaciones con estado `PENDING`.
- Compilación del proyecto.
- Página web y catálogo de bicicletas.
- Compra realizada desde la página.
- Registros de PaymentWorker y EmailWorker.

Los documentos de investigación SOA y comparación de gestores de mensajería se encuentran en:

`docs/reportes/`

## 15. Resumen de comprobaciones

| Requisito | Cómo se comprueba |
|---|---|
| Frontend para realizar pedidos | Compra desde `http://localhost:8080` |
| OrderProducer | Registro `ORDER_PUBLISHED` |
| Publicación de JSON | Respuesta de la API y mensaje en RabbitMQ |
| PaymentWorker | Registros `PAYMENT_RECEIVED` y `PAYMENT_ATTEMPT` |
| Probabilidad de fallo del 50 % | Ejecución en modo `random` |
| Tres reintentos | Ejecución en modo `failure` |
| Dead-Letter Queue | Mensaje nuevo en `bikestore.dlq` |
| EmailWorker con estado PAID | Registro `EMAIL_SENT_SIMULATED` |
| Bloqueo de confirmaciones PENDING | Registro `EMAIL_SKIPPED_NOT_PAID` |
| pedidoId, timestamp y thread | Registros en consola |
| Proyecto ejecutable | Comando `.\mvnw.cmd clean verify` |

## 16. Detener la aplicación

Para detener Spring Boot:

`Ctrl + C`

Para volver al modo de pagos aleatorios:

```powershell
Remove-Item Env:\BIKESTORE_PAYMENT_MODE -ErrorAction SilentlyContinue
```

Para detener RabbitMQ:

```powershell
docker compose down
```

Esto detiene el contenedor, pero conserva los datos del volumen de RabbitMQ.

---

## Conclusión

Con estas pruebas se comprobó que BikeStore Async v1.0 permite realizar pedidos desde una página web y procesarlos de manera asíncrona mediante RabbitMQ.

OrderProducer publica los pedidos, PaymentWorker simula los pagos con reintentos y una Dead-Letter Queue, y EmailWorker genera confirmaciones simuladas únicamente cuando el pago está aprobado.

Los registros permiten seguir cada pedido durante todo el procesamiento y comprobar el funcionamiento de los componentes solicitados en la tarea.