# Order Service

Microsserviço de pedido construído com **Spring Boot 4.0.4**, **Apache Kafka** e **PostgreSQL**, seguindo a arquitetura hexagonal (Ports & Adapters).

---

## 📋 O que o projeto faz

| Funcionalidade | Descrição |
|---|---|
| **Listar pedidos** | Retorna todos os pedidos cadastrados |
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
│   │   ├── ListOrdersUseCase   ← lista todos os pedidos
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

src/main/resources/
├── application.properties      # Configuração de desenvolvimento local
├── application-prod.properties # Overrides para produção (usa variáveis de ambiente)
└── schema.sql                  # DDL — cria a tabela orders automaticamente no startup
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

Cliente → GET /api/orders
            ↓
    ListOrdersUseCase → retorna lista de todos os pedidos
```

---

## 🔧 Pré-requisitos

Antes de rodar o projeto, você precisa ter instalado:

- **Java 21** ([Download](https://adoptium.net/pt-BR/temurin/releases?version=21&os=any&arch=any))
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

> **Nota:** O arquivo `src/main/resources/application.properties` já contém todos os valores padrão para desenvolvimento local — porta `5433`, Kafka em `localhost:9092`, usuário `postgres` e senha `postgres`. Nenhuma configuração extra é necessária para rodar localmente. Para usar credenciais diferentes, defina `PGUSER` e `PGPASSWORD` como variáveis de ambiente do sistema.

### Passo 2 — Execute a aplicação

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

A aplicação iniciará na porta **8081**.

---

## 🐳 Deploy com Docker

### Build da imagem

```powershell
# 1. Gera o JAR
.\mvnw.cmd clean package -DskipTests

# 2. Constrói a imagem Docker (usa o Dockerfile na raiz)
docker build -t order-service:latest .
```

### Executar o container

> **Pré-requisito:** o `docker-compose up -d` já deve estar rodando (PostgreSQL + Kafka).

```powershell
docker run -d `
  --name order-service `
  --network order-service_default `
  -p 8081:8081 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e DB_URL=jdbc:postgresql://postgres:5432/orderdb `
  -e PGUSER=postgres `
  -e PGPASSWORD=postgres `
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 `
  order-service:latest
```

> **Por que `kafka:29092` e não `kafka:9092`?**  
> O Kafka tem dois listeners:
> - `PLAINTEXT_HOST://localhost:9092` — para conexões **do host** (Spring Boot local). O Kafka anuncia `localhost:9092` nos metadados. Dentro de um container, `localhost:9092` resolve para o próprio container, **não para o Kafka** → `Connection refused`.
> - `PLAINTEXT://kafka:29092` — para conexões **entre containers**. Anuncia `kafka:29092`, que resolve corretamente via DNS do Docker.
>
> **Por que `--network order-service_default`?**  
> O Docker Compose cria uma rede isolada para os serviços. Sem ela, o container da app não enxerga os hostnames `kafka` e `postgres`. O nome padrão da rede é `{pasta}_default`.

### Deploy completo com docker-compose (infra + app)

Adicione o serviço `app` ao `docker-compose.yml`:

```yaml
app:
  image: order-service:latest
  depends_on:
    postgres:
      condition: service_healthy
    kafka:
      condition: service_healthy
  ports:
    - "8081:8081"
  environment:
    SPRING_PROFILES_ACTIVE: prod
    DB_URL: jdbc:postgresql://postgres:5432/orderdb
    PGUSER: ${PGUSER:-postgres}
    PGPASSWORD: ${PGPASSWORD:-postgres}
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
```

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

### Listar todos os pedidos

```http
GET http://localhost:8081/api/orders
```

**Resposta (200 OK):**
```json
[
  {
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "cliente-123",
    "amount": 299.90,
    "status": "PROCESSED",
    "createdAt": "2026-03-25T13:00:00Z"
  }
]
```

---

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

### Inventário de testes

| Classe de Teste | Tipo | Cenários cobertos |
|---|---|---|
| `OrderTest` | Unitário | Criação com status PENDING, IDs únicos, rejeição de customerId nulo, amount zero e negativo, transição para PROCESSED, rejeição de duplo processamento |
| `CreateOrderUseCaseImplTest` | Unitário | Ordem de save → publish, status PENDING retornado, mesmo ID salvo e publicado, wrapping de InterruptedException, falha de publicação, null customerId |
| `ProcessOrderUseCaseImplTest` | Unitário | Processamento e atualização de status, idempotência (evento duplicado ignorado), OrderNotFoundException para ID desconhecido |
| `FindOrderUseCaseImplTest` | Unitário | Pedido encontrado por ID, Optional vazio quando não encontrado |
| `GetOrderStatusUseCaseImplTest` | Unitário | Status PENDING, status PROCESSED, Optional vazio quando não encontrado |
| `ListOrdersUseCaseImplTest` | Unitário | Lista com 2 pedidos retornada corretamente, lista vazia quando sem pedidos |
| `OrderControllerTest` | Unitário | POST delega ao use case e retorna PENDING, GET `/{id}` 200 com body completo, GET `/{id}` 404, GET `/{id}/status` 200, GET `/{id}/status` 404, GET `/` lista com 2 pedidos, GET `/` lista vazia, propagação de EventPublishingException |
| `GlobalExceptionHandlerTest` | Unitário | 400 ConstraintViolationException, 400 MethodArgumentNotValidException com lista de erros, 503 EventPublishingException sem expor "Kafka", 405 com método e link docs, 404 NoResourceFoundException com path e link docs, 404 OrderNotFoundException com ID na mensagem, 500 genérico sem stack trace |
| `KafkaOrderEventPublisherTest` | Unitário | orderId como chave do ProducerRecord, headers `source` e `eventType` em UTF-8, CompletableFuture falho em erro do broker |
| `OrderConsumerTest` | Unitário | Processamento com incremento de `success`, exceção relançada com incremento de `error`, `success` nunca incrementado em falha, contador `dlt` incrementado no DLT handler |
| `OrderEntityTest` | Unitário | Mapeamento completo domínio → entidade → domínio (round-trip), preservação do status PROCESSED, reconstituição de pedido PENDING |
| `OrderReadRepositoryAdapterTest` | Unitário | `findById` com order mapeado, `findById` retorna vazio, `existsByIdAndStatus` true e false, `findAll` com 2 pedidos, `findAll` vazio |
| `OrderWriteRepositoryAdapterTest` | Unitário | `save` delega ao JPA, `updateStatus` com PROCESSED e com FAILED |
| `RootControllerTest` | Unitário | Redirect para `/swagger-ui.html` |
| `OrderServiceApplicationTests` | Integração | Context load com PostgreSQL e Kafka reais via Testcontainers (`ConfluentKafkaContainer` — KRaft) |

### Cobertura de testes

| Pacote / Classe | Classes | Linhas | Observação |
|---|---|---|---|
| **`application`** | **100%** | **~99%** | |
| `adapter.in.kafka` — `OrderConsumer` | 100% | 100% | |
| `adapter.in.web` — `OrderController` | 100% | 100% | Todos os endpoints incluindo `listOrders()` |
| `adapter.in.web` — `GlobalExceptionHandler` | 100% | 100% | 7 handlers testados individualmente |
| `adapter.in.web` — `RootController` | 100% | 100% | |
| `adapter.out.kafka` — `KafkaOrderEventPublisher` | 100% | 100% | |
| `adapter.out.persistence` — `OrderReadRepositoryAdapter` | 100% | 100% | Inclui `findAll()` |
| `adapter.out.persistence` — `OrderWriteRepositoryAdapter` | 100% | 100% | |
| `adapter.out.persistence` — `OrderEntity` | 100% | 100% | |
| `usecase` — `CreateOrderUseCaseImpl` | 100% | 100% | |
| `usecase` — `FindOrderUseCaseImpl` | 100% | 100% | |
| `usecase` — `GetOrderStatusUseCaseImpl` | 100% | 100% | |
| `usecase` — `ListOrdersUseCaseImpl` | 100% | 100% | |
| `usecase` — `ProcessOrderUseCaseImpl` | 100% | 100% | |
| **`domain`** | **100%** | **100%** | |
| **`infrastructure`** | **~85%** | **~96%** | `KafkaStreamsConfig`, `OpenApiConfig` não testadas |
| `OrderServiceApplication` | 0% | 0% | Coberto apenas no teste de integração |


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

| Propriedade | Padrão (dev) | Descrição |
|---|---|---|
| `server.port` | `8081` | Porta da aplicação |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/orderdb` | URL do banco de dados |
| `spring.datasource.username` | `postgres` | Usuário do banco — sobrescreva com `$PGUSER` |
| `spring.datasource.password` | `postgres` | Senha do banco — sobrescreva com `$PGPASSWORD` |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Endereço do Kafka |
| `kafka.topics.orders` | `orders-topic` | Tópico principal de pedidos |
| `kafka.partitions` | `3` | Número de partições dos tópicos |
| `kafka.replicas` | `1` | Fator de replicação (1 para dev local) |

Para produção, use o perfil `prod` com variáveis de ambiente (veja `application-prod.properties`):
```bash
KAFKA_BOOTSTRAP_SERVERS=kafka:29092   # listener interno Docker (PLAINTEXT)
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

## 🔍 Qualidade de Código (SonarQube)

O projeto foi revisado contra as regras do SonarQube. Todas as violações encontradas foram corrigidas:

| Regra | Severidade | Arquivo | Correção aplicada |
|---|---|---|---|
| **java:S2057** — Serializable sem `serialVersionUID` | Major | `OrderNotFoundException` | Adicionado `private static final long serialVersionUID = 1L` |
| **java:S2057** — Serializable sem `serialVersionUID` | Major | `EventPublishingException` | Adicionado `private static final long serialVersionUID = 1L` |
| **java:S121** — `if` sem chaves `{}` | Major | `Order` | Adicionadas chaves em `create()` e `markAsProcessed()` |
| **java:S1213** — Múltiplos statements por linha | Minor | `Order` | Cada atribuição do construtor em linha própria |
| **java:S2221** — `catch (Exception)` desnecessário | Major | `OrderConsumer` | Trocado por `catch (RuntimeException)` |
| **java:S6830** — Injeção via campo (`@Value`) | Major | `OrderConsumer` | `ordersTopic` movido para injeção por construtor, campo agora `final` |

> **Security Hotspot — java:S5146** (`RootController.redirectToSwagger`): o SonarQube marca redirects como hotspot de revisão. Como a URL `/swagger-ui.html` é **hardcoded** e não controlada pelo usuário, não há risco de open redirect. Marcar como "revisado" no painel do SonarQube.

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

### App container não conecta ao KafkaSintoma: `Connection refused` ou `LEADER_NOT_AVAILABLE` mesmo com Kafka saudável.

Causa: o container da app está usando `kafka:9092` (listener `PLAINTEXT_HOST` que anuncia `localhost:9092`). Dentro do container, `localhost` aponta para o próprio container, não para o Kafka.

```
PLAINTEXT_HOST://localhost:9092  ← apenas para Spring Boot rodando no HOST
PLAINTEXT://kafka:29092          ← para containers na mesma rede Docker
```

Solução: garantir `KAFKA_BOOTSTRAP_SERVERS=kafka:29092` e `--network order-service_default` no `docker run`.

### Schema não foi criado no banco

O `schema.sql` é executado automaticamente no startup via `spring.sql.init.mode=always`. Se a tabela `orders` não existir, o log mostrará:

```
Executing SQL script from class path resource [schema.sql]
```

Se isso não aparecer, verifique se `spring.jpa.defer-datasource-initialization=true` está ativo e se a conexão com o banco está saudável (`/actuator/health`).

