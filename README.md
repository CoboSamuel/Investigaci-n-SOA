# BikeStore Async v1.0

Proyecto desarrollado para la materia de Diseño y Arquitectura de Software.

**Autor:** Samuel Cobo

## Descripción

BikeStore Async es una aplicación en Java que busca simular el procesamiento asíncrono de pedidos de una tienda de bicicletas.

La idea es utilizar RabbitMQ para que los pedidos, los pagos y las notificaciones puedan procesarse de forma independiente, sin que el cliente tenga que esperar a que termine todo el proceso.

## Tecnologías utilizadas

- Java 21
- Spring Boot 4.1.2
- Maven
- RabbitMQ
- Docker
- Git y GitHub

## Estructura del proyecto

El código se organiza según la responsabilidad de cada componente:

- `config`: configuración de RabbitMQ y sus colas.
- `controller`: endpoints REST para recibir pedidos.
- `model`: clases que representan los pedidos y mensajes.
- `producer`: publicación de pedidos en RabbitMQ.
- `worker`: procesamiento de pagos y notificaciones.
- `docs`: reportes y evidencias del trabajo.

## Funcionamiento previsto

El sistema tendrá el siguiente flujo:

1. El cliente realiza un pedido de una bicicleta.
2. OrderProducer recibe la solicitud y publica un JSON en RabbitMQ.
3. PaymentWorker procesa el pago con una probabilidad de fallo del 50 %.
4. Si el pago falla, se realizarán hasta tres reintentos.
5. Si se agotan los reintentos, el mensaje se enviará a una Dead-Letter Queue.
6. Si el pago se completa, EmailWorker simulará el envío del correo de confirmación.

Cada operación registrará el identificador del pedido, la fecha y hora, y el hilo de ejecución.

## Requisitos

Para ejecutar el proyecto se necesita:

- JDK 21
- Docker Desktop
- Una terminal de PowerShell o VS Code

## Instalación y ejecución

### 1. Preparar RabbitMQ

Crear el archivo `.env` tomando como base `.env.example`:

    Copy-Item .env.example .env

Iniciar el contenedor:

    docker compose up -d

Verificar su estado:

    docker compose ps

El panel de administración estará disponible en:

http://localhost:15672

Las credenciales de desarrollo se encuentran en el archivo `.env`.

### 2. Ejecutar Spring Boot

Desde la carpeta principal del repositorio:

    .\mvnw.cmd spring-boot:run

Spring Boot iniciará el servidor HTTP en el puerto 8080.

Por ahora no existe un endpoint de pedidos. Se agregará en las siguientes etapas.

### 3. Detener los servicios

Para detener Spring Boot, presionar Ctrl + C en la terminal.

Para detener RabbitMQ:

    docker compose down

## Estado del desarrollo

- [x] Configuración inicial de RabbitMQ con Docker.
- [x] Creación y ejecución inicial del proyecto Spring Boot.
- [x] Conexión de Spring Boot con RabbitMQ.
- [ ] Publicación de pedidos mediante OrderProducer.
- [ ] Procesamiento de pagos con PaymentWorker.
- [ ] Implementación de reintentos y Dead-Letter Queue.
- [ ] Confirmación de pagos con EmailWorker.
- [ ] Pruebas y evidencias del funcionamiento.

## Documentación

En `docs/reportes` se guardarán los documentos de investigación SOA y comparación de gestores de mensajería.

En `docs/evidencias` se guardarán las capturas de las pruebas de la aplicación.
