# Order Service

> Serviço de gerenciamento de pedidos — recebe pedidos via HTTP, processa de forma assíncrona com Apache Kafka e salva no banco de dados PostgreSQL.

---

## Sumário

- [O que este projeto faz?](#o-que-este-projeto-faz)
- [Como funciona por dentro?](#como-funciona-por-dentro)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Estrutura de pastas](#estrutura-de-pastas)
- [O que você precisa instalar antes](#o-que-você-precisa-instalar-antes)
- [Configurações do projeto](#configurações-do-projeto)
- [Passo a passo para rodar o projeto](#passo-a-passo-para-rodar-o-projeto)
- [Endpoints da API](#endpoints-da-api)
- [Tópicos do Kafka](#tópicos-do-kafka)
- [Monitoramento](#monitoramento)
- [Rodando os testes](#rodando-os-testes)
- [Ambientes (dev vs produção)](#ambientes-dev-vs-produção)

---

## O que este projeto faz?

O **Order Service** é um microsserviço responsável por **receber e processar pedidos**. Em termos simples:

1. Um cliente (outro sistema ou você via Swagger) **envia um pedido** com o ID do cliente e o valor
2. O serviço **salva o pedido** no banco de dados com o status `PENDING` (pendente)
3. Um evento é **publicado no Kafka** — pense no Kafka como uma fila de mensagens
4. O próprio serviço **lê essa fila** e marca o pedido como `PROCESSED` (processado)
5. Se algo falhar, o sistema **tenta novamente automaticamente** até 4 vezes antes de desistir

---

## Como funciona por dentro?

O projeto segue a **Arquitetura Hexagonal**, que é uma forma de organizar o código para que as regras de negócio (o "núcleo") não dependam de detalhes externos como banco de dados ou Kafka. Isso facilita testes e manutenção.

```
                     ┌──────────────────────────┐
  Você/outro         │   NÚCLEO DO SISTEMA       │
  sistema    ──────► │                           │ ──────► Banco de dados
  (REST/HTTP)        │  Regras de negócio:       │         (PostgreSQL)
                     │  - Criar pedido           │
  Kafka       ──────► │  - Processar pedido      │ ──────► Kafka
  (fila)             │  - Consultar status       │         (publicar evento)
                     └──────────────────────────┘
                              │
                     ┌────────▼──────────────────┐
                     │  Kafka Streams             │
                     │  (calcula métricas em      │
                     │  tempo real automaticamente)│
                     └───────────────────────────┘
```

### O que acontece quando você cria um pedido?

```
1. Você envia:  POST /api/orders  { "customerId": "abc", "amount": 100 }
                        │
2. O serviço salva no banco de dados  (status: PENDING)
                        │
3. O serviço publica um evento no Kafka  (fila: orders-topic)
                        │
4. Retorna imediatamente:  202 Accepted  { "orderId": "...", "status": "PENDING" }

--- em paralelo, de forma assíncrona ---

5. O Kafka Consumer lê o evento da fila
                        │
6. Atualiza o status no banco para PROCESSED
                        │
7. Se der erro → tenta mais 3 vezes → se ainda falhar → vai para o DLT (fila de mortos)
```

---

## Tecnologias utilizadas

| Tecnologia | Para que serve neste projeto |
|---|---|
| **Java 21** | Linguagem de programação |
| **Spring Boot 3.5** | Framework que facilita criar APIs em Java |
| **Apache Kafka** | Fila de mensagens assíncrona — garante que os eventos não se percam |
| **Kafka Streams** | Processa os eventos do Kafka em tempo real para gerar métricas |
| **PostgreSQL 16** | Banco de dados relacional onde os pedidos são salvos |
| **Spring Data JPA** | Camada que facilita salvar e buscar dados no banco |
| **Micrometer + Prometheus** | Coleta métricas da aplicação (tempo de resposta, erros, etc.) |
| **SpringDoc / Swagger** | Gera automaticamente uma página web para testar a API |
| **Docker** | Permite rodar o Kafka e o PostgreSQL sem instalar nada manualmente |

---

## Estrutura de pastas

Abaixo as pastas mais importantes e o que cada uma contém:

```
src/main/java/com/olivertech/orderservice/
│
├── domain/                  ← O "coração" do sistema. Sem Spring, sem banco, sem Kafka.
│   ├── model/               │  Só as regras de negócio puras.
│   │   ├── Order.java       │  Representa um pedido
│   │   └── OrderStatus.java │  PENDING, PROCESSED ou FAILED
│   └── port/
│       ├── in/              │  O que o sistema aceita fazer (casos de uso)
│       └── out/             │  O que o sistema precisa de fora (banco, kafka)
│
├── application/
│   ├── usecase/             ← Implementação dos casos de uso (criar, processar, consultar)
│   ├── dto/                 ← Objetos que trafegam na API (request, response, evento)
│   └── adapter/
│       ├── in/web/          ← Controller REST (recebe chamadas HTTP)
│       ├── in/kafka/        ← Consumer Kafka (lê da fila)
│       └── out/             ← Adapters de saída: banco de dados e Kafka producer
│
└── infrastructure/
    ├── config/              ← Configurações do Kafka (tópicos, propriedades, serdes)
    └── streams/             ← Pipeline de métricas em tempo real com Kafka Streams
```

---

## O que você precisa instalar antes

Antes de rodar o projeto, instale:

### 1. Java 21
- Acesse: https://adoptium.net/
- Baixe o **JDK 21** para o seu sistema operacional
- Após instalar, confirme no terminal:
  ```bash
  java -version
  # Deve mostrar: openjdk version "21..."
  ```

### 2. Docker Desktop
- Acesse: https://www.docker.com/products/docker-desktop/
- Instale e **abra o Docker Desktop** antes de continuar
- Confirme que está rodando:
  ```bash
  docker -v
  # Deve mostrar: Docker version 2x.x.x...
  ```

> **Por que o Docker?** Ele vai rodar o PostgreSQL e o Kafka automaticamente em containers, sem que você precise instalar essas ferramentas manualmente na sua máquina.

---

## Configurações do projeto

O projeto já vem configurado para rodar localmente sem precisar definir nada. As configurações padrão são:

| O que é | Valor padrão | Onde fica |
|---|---|---|
| Endereço do banco | `localhost:5432` | `application.properties` |
| Nome do banco | `orderdb` | `docker-compose.yml` |
| Usuário do banco | variável `PGUSER` | arquivo `.env` (você vai criar) |
| Senha do banco | variável `PGPASSWORD` | arquivo `.env` (você vai criar) |
| Endereço do Kafka | `localhost:9092` | `application.properties` |

---

## Passo a passo para rodar o projeto

### Passo 1 — Clone ou abra o projeto

Abra o terminal na pasta raiz do projeto (onde fica o arquivo `pom.xml`).

### Passo 2 — Crie o arquivo `.env`

Este arquivo guarda as credenciais do banco de dados. Crie-o na raiz do projeto:

**Windows (PowerShell):**
```powershell
"PGUSER=postgres" | Out-File -Encoding utf8 .env
"PGPASSWORD=postgres" | Add-Content .env
```

**Linux / macOS:**
```bash
echo "PGUSER=postgres" > .env
echo "PGPASSWORD=postgres" >> .env
```

> O arquivo `.env` deve ficar na mesma pasta que o `docker-compose.yml`.

### Passo 3 — Suba o banco de dados e o Kafka

```bash
docker compose up -d
```

Aguarde alguns segundos. Para confirmar que tudo subiu:

```bash
docker compose ps
```

Você deve ver 3 serviços com o status **running**:

| Container | Porta | O que é |
|---|---|---|
| `postgres` | `5432` | Banco de dados |
| `kafka` | `9092` | Broker de mensagens |
| `kafka-ui` | `8090` | Painel web do Kafka |

> ⚠️ **Problema ao subir?** Verifique se o Docker Desktop está aberto e se as portas 5432, 9092 e 8090 não estão sendo usadas por outro programa.

### Passo 4 — Inicie a aplicação

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
./mvnw spring-boot:run
```

> A primeira execução pode demorar alguns minutos pois o Maven irá baixar as dependências.

Quando a aplicação estiver pronta, você verá no terminal algo como:
```
Started OrderServiceApplication in X.XXX seconds
```

### Passo 5 — Verifique se está funcionando

Abra o terminal e execute:

```bash
curl http://localhost:8080/actuator/health
```

Se a resposta for `{"status":"UP"}`, está tudo certo! ✅

---

## Acessando as ferramentas

| Ferramenta | URL | O que você pode fazer |
|---|---|---|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Testar a API direto no navegador |
| **Kafka UI** | http://localhost:8090 | Ver mensagens nas filas, monitorar consumers |
| **Health Check** | http://localhost:8080/actuator/health | Ver se a aplicação está saudável |
| **Métricas** | http://localhost:8080/actuator/metrics | Ver métricas de performance |

---

## Endpoints da API

### Criar um pedido

```
POST http://localhost:8080/api/orders
```

**Corpo da requisição (JSON):**
```json
{
  "customerId": "cust-abc-123",
  "amount": 299.90
}
```

| Campo | Obrigatório | Descrição |
|---|---|---|
| `customerId` | Sim | Identificador do cliente (qualquer texto não vazio) |
| `amount` | Sim | Valor do pedido em reais — deve ser maior que zero |

**Resposta de sucesso — `202 Accepted`:**
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING"
}
```

> O status começa como `PENDING` e muda para `PROCESSED` em instantes, após o Kafka processar o evento.

**Erros possíveis:**

| Código HTTP | Significa |
|---|---|
| `400 Bad Request` | Algum campo está faltando ou inválido |
| `503 Service Unavailable` | O Kafka está temporariamente fora do ar |

---

### Consultar o status de um pedido

```
GET http://localhost:8080/api/orders/{id}/status
```

Substitua `{id}` pelo UUID retornado na criação do pedido.

**Exemplo:**
```
GET http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/status
```

**Resposta de sucesso — `200 OK`:**
```json
"PROCESSED"
```

**Resposta quando não encontrado — `404 Not Found`:** pedido com este ID não existe.

> O `id` precisa ser um UUID v4 válido (formato: `xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxxx`).

---

### Testando pelo Swagger (mais fácil)

Acesse **http://localhost:8080/swagger-ui.html** no navegador.
Você verá uma interface visual onde pode preencher os campos e clicar em **Execute** para testar os endpoints sem precisar do terminal.

---

## Tópicos do Kafka

O Kafka organiza as mensagens em **tópicos** (pense como "canais" ou "filas com nome"). Este projeto usa:

| Tópico | Para que serve |
|---|---|
| `orders-topic` | Recebe o evento quando um pedido é criado |
| `orders-topic-retry-0` | Se o processamento falhar, a mensagem vai aqui (1ª nova tentativa) |
| `orders-topic-retry-1` | 2ª nova tentativa |
| `orders-topic-retry-2` | 3ª nova tentativa |
| `orders-topic.DLT` | **Dead Letter Topic** — se falhar 4 vezes, a mensagem fica aqui para análise |
| `order-metrics-topic` | Métricas geradas pelo Kafka Streams (quantidade por status, receita, etc.) |

### Como funciona o retry (nova tentativa automática)?

```
Processamento falhou na 1ª vez
    └─► Aguarda 2 segundos → tenta de novo
            └─► Falhou de novo → aguarda 4 segundos → tenta de novo
                    └─► Falhou de novo → aguarda 8 segundos → tenta de novo
                            └─► Falhou de novo → vai para o DLT (para análise manual)
```

> Se o erro for de **formato inválido da mensagem** (desserialização), ela vai direto para o DLT — não adianta tentar de novo.

Você pode ver as mensagens no DLT acessando o **Kafka UI** em http://localhost:8090.

---

## Monitoramento

### Status da aplicação

```
GET http://localhost:8080/actuator/health
```

Mostra se a aplicação e suas dependências (banco, kafka) estão funcionando.

### Métricas coletadas automaticamente

| Métrica | O que mede |
|---|---|
| `kafka.consumer.processing.time` | Quanto tempo leva para processar cada mensagem do Kafka |
| `kafka.consumer.success` | Quantas mensagens foram processadas com sucesso |
| `kafka.consumer.dlt` | Quantas mensagens foram para o Dead Letter Topic |
| `http.server.requests` | Tempo e volume das chamadas à API REST |

Todas as métricas estão disponíveis em formato Prometheus em:
```
GET http://localhost:8080/actuator/prometheus
```

---

## Rodando os testes

```powershell
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

> ⚠️ **O Docker precisa estar rodando** para os testes funcionarem, pois eles sobem containers reais de PostgreSQL e Kafka automaticamente via **Testcontainers**.

Os testes verificam:
- **Testes unitários** — as regras de negócio funcionam corretamente (sem banco, sem Kafka)
- **Testes de integração** — o fluxo completo funciona com banco e Kafka reais

---

## Ambientes (dev vs produção)

O projeto tem dois ambientes configurados:

### Desenvolvimento (padrão)
- Ativo quando você roda normalmente com `spring-boot:run`
- O banco de dados é **recriado do zero** a cada reinício (`ddl-auto=create`)
- O Swagger fica **habilitado**
- As variáveis de conexão têm valores padrão (não precisam ser definidas obrigatoriamente)

> ⚠️ No ambiente de desenvolvimento, todos os dados do banco são apagados a cada reinício da aplicação!

### Produção
- Ativo quando você define `SPRING_PROFILES_ACTIVE=prod`
- O banco **não é recriado** — apenas valida se o schema está correto (`ddl-auto=validate`)
- O Swagger fica **desabilitado** (por segurança)
- Todas as variáveis de ambiente são **obrigatórias** (sem valores padrão)

Para rodar em modo produção:
```bash
java -jar target/order-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Parando tudo

Para parar a aplicação, pressione `Ctrl + C` no terminal onde ela está rodando.

Para parar os containers Docker:

```bash
# Para os containers (mantém os dados do banco)
docker compose down

# Para os containers E apaga todos os dados do banco
docker compose down -v
```
