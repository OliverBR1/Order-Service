# Order Service

Microsserviço de pedidos construído com **Spring Boot 3.5**, **Apache Kafka** e **PostgreSQL**, seguindo a arquitetura hexagonal (Ports & Adapters).

---

## 📋 O que o projeto faz

| Funcionalidade | Descrição |
|---|---|
| **Criar pedido** | Recebe um pedido via REST, persiste no banco e publica um evento no Kafka |
| **Buscar pedido** | Retorna os dados completos de um pedido pelo ID |
| **Consultar status** | Retorna apenas o status atual de um pedido pelo ID |
| **Processar pedido** | Consome o evento Kafka e atualiza o status para `PROCESSED` |
| **Métricas em tempo real** | Pipeline Kafka Streams que agrega pedidos por status |
| **Retry automático** | Tentativas com backoff exponencial + Dead Letter Topic (DLT) |
| **Observabilidade** | Métricas Prometheus via Actuator |

---

## 🏗️ Arquitetura

```
src/main/java/com/olivertech/orderservice/
├── domain/                     # Regras de negócio puras (sem Spring)
│   ├── model/                  # Order, OrderStatus
│   ├── exception/              # OrderNotFoundException, EventPublishingException
│   ├── port/in/                # Interfaces de entrada (use cases)
│   │   ├── CreateOrderUseCase
│   │   ├── FindOrderUseCase    ← busca pedido completo por ID
│   │   ├── GetOrderStatusUseCase
│   │   └── ProcessOrderUseCase
│   └── port/out/               # Interfaces de saída (repositório, Kafka)
│       ├── OrderReadRepositoryPort
│       ├── OrderWriteRepositoryPort
│       └── OrderEventPublisherPort
├── application/                # Implementação dos use cases
│   ├── usecase/                # CreateOrder, FindOrder, ProcessOrder, GetOrderStatus
│   ├── adapter/in/             # HTTP (REST) e Kafka Consumer
│   ├── adapter/out/            # Kafka Producer e JPA Repository
│   └── dto/                    # OrderRequest, OrderResponse, OrderEvent
└── infrastructure/             # Configurações técnicas
    ├── config/                 # Kafka, OpenAPI, Topics
    └── streams/                # Pipeline Kafka Streams de métricas
```

### Fluxo completo de um pedido

```
Cliente → POST /api/orders
            ↓
    CreateOrderUseCase
    ├── Persiste no PostgreSQL (status=PENDING)
    └── Publica evento no Kafka (orders-topic)
            ↓
    OrderConsumer (Kafka Listener)
            ↓
    ProcessOrderUseCase
    └── Atualiza status para PROCESSED no PostgreSQL

Cliente → GET /api/orders/{id}
            ↓
    FindOrderUseCase → retorna dados completos do pedido
```

---

## 🔧 Pré-requisitos

Antes de rodar o projeto, você precisa ter instalado:

- **Java 21** ([Download](https://adoptium.net/))
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop/))
- **Maven 3.9+** (ou use o `./mvnw` incluído no projeto)

---

## 🚀 Como executar o projeto

### Passo 1 — Suba o banco de dados e o Kafka

```bash
docker-compose up -d
```

Aguarde todos os serviços ficarem saudáveis (leva ~30 segundos para o Kafka):

```bash
docker-compose ps
# Aguarde todos os STATUS mostrarem "healthy"
```

> **Nota:** O arquivo `src/main/resources/application.properties` já contém todos os valores padrão para desenvolvimento local (porta `5433`, usuário `postgres`, Kafka em `localhost:9092`). Nenhuma configuração extra é necessária para rodar localmente.

### Passo 2 — Execute a aplicação

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

A aplicação iniciará na porta **8081**.

---

## 🌐 Acessando as interfaces

| Interface | URL | Descrição |
|---|---|---|
| **API REST** | http://localhost:8081/api/orders | Endpoints de pedidos |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | Documentação interativa da API |
| **Actuator Health** | http://localhost:8081/actuator/health | Status da aplicação |
| **Métricas Prometheus** | http://localhost:8081/actuator/prometheus | Métricas para monitoramento |
| **Kafka UI** | http://localhost:8090 | Painel visual dos tópicos Kafka |

---

## 📡 Endpoints da API

### Criar um pedido

```http
POST http://localhost:8081/api/orders
Content-Type: application/json

{
  "customerId": "cliente-123",
  "amount": 299.90
}
```

**Resposta (202 Accepted):**
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "cliente-123",
  "amount": 299.90,
  "status": "PENDING",
  "createdAt": "2026-03-25T13:00:00Z"
}
```

---

### Buscar pedido completo

```http
GET http://localhost:8081/api/orders/{id}
```

**Resposta (200 OK):**
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "cliente-123",
  "amount": 299.90,
  "status": "PROCESSED",
  "createdAt": "2026-03-25T13:00:00Z"
}
```

**Resposta (404 Not Found)** — quando o ID não existe.

---

### Consultar apenas o status do pedido

```http
GET http://localhost:8081/api/orders/{id}/status
```

**Resposta (200 OK):**
```
"PROCESSED"
```

**Resposta (404 Not Found)** — quando o ID não existe.

---

## 🧪 Executando os testes

```powershell
# Todos os testes (unitários + integração com Testcontainers)
.\mvnw.cmd test

# Apenas uma classe de teste
.\mvnw.cmd test -Dtest=CreateOrderUseCaseImplTest
```

> **Nota:** Os testes unitários não precisam de Docker. São isolados com Mockito.  
> O teste de integração (`OrderServiceApplicationTests`) usa **Testcontainers** e requer Docker em execução.

### Cobertura de testes

| Classe de Teste | Tipo | O que valida |
|---|---|---|
| `OrderTest` | Unitário | Regras de domínio (criação, transições de status) |
| `CreateOrderUseCaseImplTest` | Unitário | Criação de pedido, publicação Kafka, validações |
| `ProcessOrderUseCaseImplTest` | Unitário | Processamento, idempotência, pedido não encontrado |
| `OrderControllerTest` | Unitário | Endpoints REST (POST, GET por ID, GET status) |
| `GlobalExceptionHandlerTest` | Unitário | Mapeamento de exceções para HTTP |
| `KafkaOrderEventPublisherTest` | Unitário | Publicação Kafka com headers e chave corretos |
| `OrderConsumerTest` | Unitário | Consumo Kafka, métricas, tratamento de erros |
| `OrderServiceApplicationTests` | Integração | Context load com PostgreSQL + Kafka reais |

---

## 🐳 Serviços Docker

| Serviço | Porta | Descrição |
|---|---|---|
| PostgreSQL | `5433` | Banco de dados principal |
| Kafka | `9092` | Broker de mensagens (KRaft, sem Zookeeper) |
| Kafka UI | `8090` | Painel web de administração do Kafka |

### Comandos úteis do Docker

```powershell
# Subir todos os serviços em background
docker-compose up -d

# Ver logs em tempo real
docker-compose logs -f kafka

# Parar todos os serviços
docker-compose down

# Parar e apagar todos os dados (banco incluído)
docker-compose down -v
```

---

## ⚙️ Configurações principais

As configurações ficam em `src/main/resources/application.properties`.

| Propriedade | Padrão | Descrição |
|---|---|---|
| `server.port` | `8081` | Porta da aplicação |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Endereço do Kafka |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/orderdb` | Banco de dados |
| `kafka.topics.orders` | `orders-topic` | Tópico principal de pedidos |
| `kafka.partitions` | `3` | Número de partições dos tópicos |
| `kafka.replicas` | `1` | Fator de replicação (1 para dev local) |

Para produção, use o perfil `prod` com variáveis de ambiente (veja `application-prod.properties`):
```bash
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
DB_URL=jdbc:postgresql://postgres:5432/orderdb
PGUSER=usuario
PGPASSWORD=senha_segura
```

---

## 📊 Tópicos Kafka

| Tópico | Descrição |
|---|---|
| `orders-topic` | Eventos de pedidos criados |
| `orders-topic.DLT` | Dead Letter Topic — pedidos que falharam após todas as retentativas |
| `orders-topic-0` | Fila de retry 1 (backoff: 2s) |
| `orders-topic-1` | Fila de retry 2 (backoff: 4s) |
| `orders-topic-2` | Fila de retry 3 (backoff: 8s) |
| `order-metrics-topic` | Métricas agregadas por status (via Kafka Streams) |

> Os nomes `orders-topic-0/1/2` seguem a convenção gerada automaticamente pelo `@RetryableTopic` com `TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE`.

---

## 🔄 Política de Retry

Quando o consumo falha, o sistema tenta novamente automaticamente:

| Tentativa | Aguarda |
|---|---|
| 1ª (original) | imediatamente |
| 2ª (`orders-topic-0`) | 2 segundos |
| 3ª (`orders-topic-1`) | 4 segundos |
| 4ª (`orders-topic-2`) | 8 segundos (máx. 30s) |
| DLT | Após esgotar todas as tentativas |

Exceções de desserialização (`DeserializationException`, `JsonParseException`) são enviadas diretamente ao DLT sem retentativas.

---

## 🏥 Health Check

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

---

## 🛠️ Troubleshooting

### Aplicação não inicia — erro de banco de dados

Verifique se o PostgreSQL está rodando:
```powershell
docker-compose ps postgres
# O status deve ser "healthy"
```

### Aplicação não inicia — erro de Kafka

Verifique se o Kafka está rodando:
```powershell
docker-compose ps kafka
# O status deve ser "healthy" (pode demorar ~30s)
```

### Porta já em uso

A aplicação usa a porta `8081`. Se houver conflito:
```powershell
# Encontra o processo na porta 8081
netstat -ano | findstr :8081
# Encerre o processo com o PID encontrado
Stop-Process -Id <PID>
```

### Limpeza de build corrompido (classes stale)

```powershell
.\mvnw.cmd clean package
```
