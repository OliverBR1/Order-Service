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

## 🔒 Segurança

### Autenticação via API Key

Todos os endpoints `/api/**` exigem o header `X-API-Key`:

```http
GET /api/orders HTTP/1.1
Host: localhost:8081
X-API-Key: dev-api-key-CHANGE-THIS-BEFORE-USE
```

| Resposta | Situação |
|---|---|
| `401 Unauthorized` | Header ausente ou chave inválida |
| `200 / 202 / 404` | Chave válida |

A chave é configurada pela propriedade `api.security.key` (via variável de ambiente `API_KEY` em produção). A comparação é feita em **tempo constante** (resistente a timing attacks).

### Rate Limiting

Limitado a **100 requisições por minuto** por cliente (identificado pela API Key ou IP de origem). Quando excedido:

```json
HTTP 429 Too Many Requests
Retry-After: 60

{ "error": "Limite de requisições excedido. Tente novamente em breve." }
```

### Security Headers (todas as respostas)

| Header | Valor (produção) |
|---|---|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'` |
| `Referrer-Policy` | `no-referrer` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` (só em prod/HTTPS) |
| `Cache-Control` | `no-store` |

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
    ├── config/                 # Kafka, OpenAPI, Topics, SecurityHeaders,
    │                           # SecurityConfig, ApiKeyAuthFilter, RateLimitFilter
    └── streams/                # Pipeline Kafka Streams de métricas

src/main/resources/
├── application.properties      # Configuração de desenvolvimento local
├── application-prod.properties # Overrides para produção (usa variáveis de ambiente)
└── schema.sql                  # DDL — cria a tabela orders automaticamente no startup
```

### Fluxo completo de um pedido

```
Cliente → POST /api/orders  (com X-API-Key)
             ↓
     RateLimitFilter → ApiKeyAuthFilter → SecurityConfig
             ↓
     CreateOrderUseCase
     ├── Persiste no PostgreSQL (status=PENDING)
     └── Publica evento no Kafka (orders-topic)
             ↓
     OrderConsumer (Kafka Listener)
             ↓
     ProcessOrderUseCase
     └── Atualiza status para PROCESSED no PostgreSQL

Cliente → GET /api/orders/{id}  (com X-API-Key)
             ↓
     FindOrderUseCase → retorna dados completos do pedido
```

---

## 🔧 Pré-requisitos

Antes de rodar o projeto, você precisa ter instalado:

- **Java 21** ([Download](https://adoptium.net/pt-BR/temurin/releases?version=21&os=any&arch=any))
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop/))
- **Maven 3.9+** (ou use o `./mvnw` incluído no projeto)

---

## 🚀 Como executar o projeto

### Passo 1 — Configure as variáveis de ambiente

Copie o arquivo de exemplo e preencha os valores:

```powershell
Copy-Item .env.example .env
# Edite o .env com suas senhas e a API Key
notepad .env
```

O `.env` é ignorado pelo Git (já está no `.gitignore`). As variáveis obrigatórias são:

| Variável | Descrição |
|---|---|
| `PGUSER` | Usuário do PostgreSQL |
| `PGPASSWORD` | Senha do PostgreSQL |
| `API_KEY` | Chave de autenticação da API (`X-API-Key`) |
| `KAFKA_UI_PASSWORD` | Senha do painel web do Kafka UI |
| `KAFKA_UI_USER` | Usuário do Kafka UI (padrão: `admin`) |

### Passo 2 — Suba o banco de dados e o Kafka

```powershell
docker-compose up -d postgres kafka kafka-ui
```

Aguarde todos os serviços ficarem saudáveis (leva ~30 segundos para o Kafka):

```powershell
docker-compose ps
# Aguarde todos os STATUS mostrarem "healthy"
```

### Passo 3 — Execute a aplicação

**Opção A — Maven local** (mais rápido para desenvolvimento):

```powershell
.\mvnw.cmd spring-boot:run
```

**Opção B — Docker completo** (simula produção):

```powershell
# 1. Gera o JAR
.\mvnw.cmd clean package -DskipTests

# 2. Sobe todos os serviços incluindo a aplicação
docker-compose up -d
```

A API estará disponível em http://localhost:8081.

> **Nota:** O PostgreSQL e o Kafka ficam acessíveis apenas em `127.0.0.1` (não expostos na rede local). O serviço `app` expõe a porta 8081 externamente para receber requisições.

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
  -e PGPASSWORD=senha_segura `
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 `
  -e API_KEY=sua-api-key-segura `
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

O `docker-compose.yml` já inclui o serviço `app` configurado para produção. Para fazer o deploy completo:

```powershell
# 1. Gere o JAR
.\mvnw.cmd clean package -DskipTests

# 2. Suba todos os serviços (infra + aplicação)
docker-compose up -d
```

O serviço `app` já está configurado com:
- `restart: unless-stopped` — reinicia automaticamente em caso de falha
- `healthcheck` — verifica `http://localhost:9090/actuator/health` a cada 30s
- `TZ: UTC` — timezone padronizado
- `depends_on: postgres/kafka: condition: service_healthy` — aguarda infra ficar pronta antes de iniciar

```yaml
# Trecho relevante do docker-compose.yml
app:
  image: order-service:latest
  build: .
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
    PGUSER: ${PGUSER:?PGUSER obrigatória}
    PGPASSWORD: ${PGPASSWORD:?PGPASSWORD obrigatória}
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    API_KEY: ${API_KEY:?API_KEY obrigatória}
    TZ: UTC
  restart: unless-stopped
```

---

## 🌐 Acessando as interfaces

| Interface | URL | Descrição |
|---|---|---|
| **API REST** | http://localhost:8081/api/orders | Endpoints de pedidos (requer `X-API-Key`) |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | Documentação interativa — **disponível apenas em dev** (desabilitado em produção) |
| **Actuator Health** | http://localhost:9090/actuator/health | Status da aplicação (apenas localhost) |
| **Métricas Prometheus** | http://localhost:9090/actuator/prometheus | Métricas para monitoramento (apenas localhost) |
| **Kafka UI** | http://localhost:8090 | Painel visual (requer login — `KAFKA_UI_USER`/`KAFKA_UI_PASSWORD`) |

> **Nota:** Actuator e Kafka UI ficam acessíveis apenas em `127.0.0.1`. A API é acessível externamente. O Swagger é desabilitado automaticamente no perfil `prod` (`springdoc.swagger-ui.enabled=false`).

---

## 📡 Endpoints da API

> Todos os endpoints `/api/**` exigem o header `X-API-Key`.

### Listar todos os pedidos

```http
GET http://localhost:8081/api/orders
X-API-Key: dev-api-key-CHANGE-THIS-BEFORE-USE
```

**Parâmetros de query opcionais:**

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| `page` | int | `0` | Número da página (0-indexed) |
| `size` | int | `20` | Itens por página (máx. 100) |

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
X-API-Key: dev-api-key-CHANGE-THIS-BEFORE-USE

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
X-API-Key: dev-api-key-CHANGE-THIS-BEFORE-USE
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

**Resposta (404 Not Found)** — mensagem genérica, sem expor o ID pesquisado.

---

### Consultar apenas o status do pedido

```http
GET http://localhost:8081/api/orders/{id}/status
X-API-Key: dev-api-key-CHANGE-THIS-BEFORE-USE
```

**Resposta (200 OK):**
```
"PROCESSED"
```

**Resposta (404 Not Found)** — mensagem genérica, sem expor o ID pesquisado.

---

## 🧪 Executando os testes

```powershell
# Todos os testes (unitários + integração com Testcontainers)
# Requer Docker Desktop em execução
.\mvnw.cmd test

# Apenas testes unitários (sem Docker)
.\mvnw.cmd test -Dtest="!OrderServiceApplicationTests"

# Uma classe específica
.\mvnw.cmd test -Dtest=CreateOrderUseCaseImplTest
```

> **Nota:** Os testes unitários são completamente isolados com Mockito e `SimpleMeterRegistry` — não requerem Docker, banco de dados ou Kafka.  
> O teste de integração (`OrderServiceApplicationTests`) usa **Testcontainers** para subir PostgreSQL e Kafka reais e requer Docker Desktop em execução.

### Inventário de testes

| Classe de Teste | Tipo | Cenários cobertos |
|---|---|---|
| `OrderTest` | Unitário | Criação com status PENDING, IDs únicos, rejeição de customerId nulo, amount zero e negativo, transição para PROCESSED, rejeição de duplo processamento |
| `CreateOrderUseCaseImplTest` | Unitário | Ordem de save → publish, status PENDING retornado, mesmo ID salvo e publicado, wrapping de InterruptedException, falha de publicação, null customerId, amount zero |
| `ProcessOrderUseCaseImplTest` | Unitário | Processamento e atualização de status, idempotência (evento duplicado ignorado), `markAsProcessed` chamado no domínio, `OrderNotFoundException` para ID desconhecido |
| `FindOrderUseCaseImplTest` | Unitário | Pedido encontrado por ID, Optional vazio quando não encontrado |
| `GetOrderStatusUseCaseImplTest` | Unitário | Status PENDING, status PROCESSED, Optional vazio quando não encontrado |
| `ListOrdersUseCaseImplTest` | Unitário | Lista completa com 2 pedidos, lista vazia, lista paginada com `page` e `size`, página vazia |
| `OrderControllerTest` | Unitário | POST delega ao use case e retorna PENDING, GET `/{id}` 200 com body completo, GET `/{id}` 404, GET `/{id}/status` 200, GET `/{id}/status` 404, GET `/` lista com 2 pedidos, GET `/` lista vazia, propagação de `EventPublishingException` |
| `GlobalExceptionHandlerTest` | Unitário | 400 `ConstraintViolationException` sem expor nome de método Java (M1), 400 `MethodArgumentNotValidException` com lista de erros, 503 `EventPublishingException` sem detalhes internos, 405 com método e link docs, 404 `NoResourceFoundException` sem expor path, 404 `OrderNotFoundException` com mensagem genérica sem ID (B4), 500 genérico sem stack trace |
| `ApiKeyAuthFilterTest` | Unitário | Autenticação com chave válida, rejeição de chave inválida, rejeição sem chave, delegação ao próximo filtro em todos os casos, resistência a timing attack com chave de mesmo tamanho |
| `RateLimitFilterTest` | Unitário | Primeira requisição permitida, bloqueio ao exceder limite, sem delegação ao próximo filtro quando bloqueado, reset do contador após janela expirar, preferência de API Key sobre IP, uso do primeiro IP do X-Forwarded-For |
| `KafkaOrderEventPublisherTest` | Unitário | `orderId` como chave do `ProducerRecord`, headers `source` e `eventType` em UTF-8, `CompletableFuture` falho em erro do broker |
| `OrderConsumerTest` | Unitário | Processamento com incremento de contador `success`, exceção relançada com incremento de `error`, `success` nunca incrementado em falha, contador `dlt` incrementado no DLT handler |
| `OrderEntityTest` | Unitário | Mapeamento completo domínio → entidade → domínio (round-trip), preservação do status PROCESSED, reconstituição de pedido PENDING |
| `OrderReadRepositoryAdapterTest` | Unitário | `findById` com order mapeado, `findById` retorna vazio, `existsByIdAndStatus` true e false, `findAll` com 2 pedidos, `findAll` vazio, `findAll` paginado com 2 pedidos, `findAll` paginado vazio |
| `OrderWriteRepositoryAdapterTest` | Unitário | `save` delega ao JPA, `updateStatus` com PROCESSED, `updateStatus` com FAILED |
| `RootControllerTest` | Unitário | Redirect para `/swagger-ui.html` |
| `SecurityHeadersFilterTest` | Unitário | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, CSP configurável via `@Value`, HSTS desabilitado em dev, HSTS habilitado em prod, `Referrer-Policy`, `Permissions-Policy`, `Cache-Control`, supressão de `X-Powered-By`/`Server`, delegação ao próximo filtro |
| `KafkaTopicConfigTest` | Unitário | Nome correto para cada um dos 6 tópicos Kafka criados pelos beans |
| `OpenApiConfigTest` | Unitário | Título, versão, descrição, dados de contato e esquema de segurança `X-API-Key` registrado |
| `KafkaStreamsConfigTest` | Unitário | Bootstrap servers, application ID, state dir, processamento EOS v2, commit interval |
| `OrderMetricsStreamTest` | Unitário | Leitura do tópico `orders-topic`, escrita no tópico `order-metrics-topic` |
| `OrderServiceApplicationTests` | **Integração** | Context load com PostgreSQL e Kafka reais via Testcontainers (KRaft, sem Zookeeper) |

**Total: 109 testes unitários + 1 teste de integração**

### Cobertura de testes

| Pacote | Instrução | Observação |
|---|---|---|
| `application.adapter.in.kafka` | **100%** | `OrderConsumer` — métricas verificadas via `SimpleMeterRegistry` |
| `application.adapter.in.web` | **99%** | `OrderController`, `GlobalExceptionHandler`, `RootController` |
| `application.adapter.out.kafka` | **100%** | `KafkaOrderEventPublisher` |
| `application.adapter.out.persistence` | **100%** | `OrderEntity`, `OrderReadRepositoryAdapter`, `OrderWriteRepositoryAdapter` |
| `application.dto` | **100%** | `OrderRequest`, `OrderResponse`, `OrderEvent` |
| `application.usecase` | **100%** | Todos os 5 use cases |
| `domain.exception` | **100%** | `EventPublishingException`, `OrderNotFoundException` |
| `domain.model` | **100%** | `Order`, `OrderStatus` |
| `infrastructure.config` | **100%** | `KafkaTopicConfig`, `KafkaStreamsConfig`, `OpenApiConfig`, `SecurityHeadersFilter`, `ApiKeyAuthFilter`, `RateLimitFilter` |
| `infrastructure.streams` | **65%** | `OrderMetricsStream` — lambdas internas da DSL Kafka Streams só executam com mensagens reais |
| `com.olivertech.orderservice` | **0%** | `OrderServiceApplication.main()` — coberto apenas no teste de integração |
| **Total geral** | **~96%** | Medido com JaCoCo (`mvn verify`) |

---

## 🐳 Serviços Docker

| Serviço | Porta | Acesso | Descrição |
|---|---|---|---|
| PostgreSQL | `127.0.0.1:5433` | Apenas localhost | Banco de dados principal |
| Kafka | `127.0.0.1:9092` | Apenas localhost | Broker de mensagens (KRaft, sem Zookeeper) |
| Kafka UI | `127.0.0.1:8090` | Apenas localhost | Painel web (requer login) |
| App | `0.0.0.0:8081` | Externo | API REST do microsserviço |

### Comandos úteis do Docker

```powershell
# Subir todos os serviços em background
$env:KAFKA_UI_PASSWORD="sua-senha"
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
| `server.port` | `8081` | Porta da API |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/orderdb` | URL do banco de dados |
| `spring.datasource.username` | `postgres` | Usuário do banco — sobrescreva com `$PGUSER` |
| `spring.datasource.password` | `postgres` | Senha do banco — sobrescreva com `$PGPASSWORD` |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Schema criado pelo `schema.sql`; Hibernate só valida (não cria tabelas) |
| `spring.jpa.defer-datasource-initialization` | `false` | `schema.sql` executa **antes** do Hibernate — obrigatório com `ddl-auto=validate` |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Endereço do Kafka |
| `kafka.topics.orders` | `orders-topic` | Tópico principal de pedidos |
| `kafka.partitions` | `3` | Número de partições dos tópicos |
| `kafka.replicas` | `1` | Fator de replicação (1 para dev local) |
| `management.server.port` | `9090` | Porta do Actuator (separada da API) |
| `management.server.address` | `127.0.0.1` | Actuator acessível apenas localmente |
| `management.endpoint.health.show-details` | `never` | Detalhes de infra nunca expostos em dev |
| `api.security.key` | `dev-api-key-CHANGE-THIS-BEFORE-USE` | Chave da API — **trocar antes de usar** |
| `security.headers.csp` | CSP permissivo para Swagger | Content-Security-Policy configurável por ambiente |
| `security.headers.hsts.enabled` | `false` | HSTS desabilitado em dev (sem TLS) |

Para produção, use o perfil `prod` com variáveis de ambiente (veja `application-prod.properties`):
```bash
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
DB_URL=jdbc:postgresql://postgres:5432/orderdb
PGUSER=usuario
PGPASSWORD=senha_segura
API_KEY=chave-segura-producao
KAFKA_UI_PASSWORD=senha-kafka-ui
```

Diferenças do perfil `prod` em relação ao dev:

| Comportamento | Dev | Prod |
|---|---|---|
| Swagger UI / API Docs | ✅ Habilitado | ❌ Desabilitado |
| `spring.sql.init.mode` | `always` | `never` |
| SSL no banco (sslmode) | — | `verify-full` |
| Actuator health details | `never` | `when-authorized` (role `ACTUATOR_ADMIN`) |
| HSTS | Desabilitado | Habilitado |
| CSP | Permissivo (Swagger) | `default-src 'none'; frame-ancestors 'none'` |

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
Invoke-RestMethod http://localhost:9090/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP"
}
```

> Em dev `show-details=never`. Para ver detalhes de componentes internos configure `show-details=always` localmente.

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

### API retornando 401 Unauthorized

Verifique se o header `X-API-Key` está sendo enviado com o valor correto:
```powershell
# Teste rápido via PowerShell
Invoke-RestMethod -Uri http://localhost:8081/api/orders `
  -Headers @{ "X-API-Key" = "dev-api-key-CHANGE-THIS-BEFORE-USE" }
```
O valor da chave em dev está em `api.security.key` no `application.properties`.

### API retornando 429 Too Many Requests

O rate limiter bloqueou após 100 requisições/minuto. Aguarde 60 segundos (indicado no header `Retry-After: 60`).

### docker-compose não sobe — KAFKA_UI_PASSWORD obrigatória

```
error: required variable KAFKA_UI_PASSWORD is not set
```

Defina a variável antes de subir:
```powershell
$env:KAFKA_UI_PASSWORD="senha-local-dev"
docker-compose up -d
```

### Limpeza de build corrompido (classes stale)

```powershell
.\mvnw.cmd clean package
```

### App container não conecta ao Kafka

Sintoma: `Connection refused` ou `LEADER_NOT_AVAILABLE` mesmo com Kafka saudável.

Causa: o container da app está usando `kafka:9092` (listener `PLAINTEXT_HOST` que anuncia `localhost:9092`). Dentro do container, `localhost` aponta para o próprio container, não para o Kafka.

```
PLAINTEXT_HOST://localhost:9092  ← apenas para Spring Boot rodando no HOST
PLAINTEXT://kafka:29092          ← para containers na mesma rede Docker
```

Solução: garantir `KAFKA_BOOTSTRAP_SERVERS=kafka:29092` e `--network order-service_default` no `docker run`.

### Schema não foi criado no banco

O `schema.sql` é executado automaticamente no startup via `spring.sql.init.mode=always` (dev). Se a tabela `orders` não existir, o log mostrará:

```
Executing SQL script from class path resource [schema.sql]
```

**Ordem de inicialização correta** (com `defer-datasource-initialization=false`):
1. `schema.sql` executa → `CREATE TABLE IF NOT EXISTS orders`
2. Hibernate valida o schema → tabela existe → ✅

Se a aplicação falhar com `SchemaManagementException: missing table [orders]`, verifique se `spring.jpa.defer-datasource-initialization=false` está configurado em `application.properties` e se a conexão com o banco está saudável (`/actuator/health`).

> **Em produção** `spring.sql.init.mode=never` — migrações devem ser feitas via Flyway/Liquibase.

### Teste de integração falha — Docker não encontrado

O `OrderServiceApplicationTests` usa Testcontainers para subir PostgreSQL e Kafka reais. Se o Docker Desktop não estiver em execução:

```
IllegalStateException: Could not find a valid Docker environment.
```

Solução: inicie o Docker Desktop e aguarde ele ficar totalmente disponível antes de rodar `.\mvnw.cmd test`. Para rodar apenas os testes unitários sem Docker:

```powershell
.\mvnw.cmd test -Dtest="!OrderServiceApplicationTests"
```

### Erros de log nos testes unitários

Mensagens como `ERROR GlobalExceptionHandler — Erro inesperado`, `WARN RateLimitFilter — Rate limit excedido` ou `ERROR OrderConsumer — [DLT]` aparecem no output dos testes são **esperadas** — fazem parte do comportamento testado. Não indicam falha.
