package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.application.dto.OrderRequest;
import com.olivertech.orderservice.application.dto.OrderResponse;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.CreateOrderUseCase;
import com.olivertech.orderservice.domain.port.in.FindOrderUseCase;
import com.olivertech.orderservice.domain.port.in.GetOrderStatusUseCase;
import com.olivertech.orderservice.domain.port.in.ListOrdersUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Endpoints de gestão de pedidos")
@SecurityRequirement(name = "X-API-Key")
@Validated
public class OrderController {

    private static final String UUID_V4_REGEXP =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE     = 100;

    private final CreateOrderUseCase    createOrderUseCase;
    private final FindOrderUseCase      findOrderUseCase;
    private final GetOrderStatusUseCase getOrderStatusUseCase;
    private final ListOrdersUseCase     listOrdersUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           FindOrderUseCase findOrderUseCase,
                           GetOrderStatusUseCase getOrderStatusUseCase,
                           ListOrdersUseCase listOrdersUseCase) {
        this.createOrderUseCase    = createOrderUseCase;
        this.findOrderUseCase      = findOrderUseCase;
        this.getOrderStatusUseCase = getOrderStatusUseCase;
        this.listOrdersUseCase     = listOrdersUseCase;
    }

    @Operation(summary = "Listar pedidos",
            description = "Retorna pedidos paginados. Máximo de 100 por página.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public List<OrderResponse> listOrders(
            @Parameter(description = "Número da página (0-indexed)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (máx. 100)")
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE)
            @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return listOrdersUseCase.execute(page, size).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Operation(summary = "Criar pedido",
            description = "Aceita pedido e publica evento Kafka de forma assíncrona.")
    @ApiResponse(responseCode = "202", description = "Pedido aceito")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "503", description = "Kafka temporariamente indisponível")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = createOrderUseCase.execute(request.customerId(), request.amount());
        return OrderResponse.from(order);
    }

    @Operation(summary = "Buscar pedido por ID",
            description = "Retorna os dados completos de um pedido.")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "UUID v4 do pedido")
            @PathVariable
            @Size(min = 36, max = 36, message = "ID deve ter exatamente 36 caracteres")
            @Pattern(regexp = UUID_V4_REGEXP, message = "ID deve ser um UUID v4 válido")
            String id) {
        return findOrderUseCase.execute(id)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Consultar status do pedido")
    @ApiResponse(responseCode = "200", description = "Status encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatus> getStatus(
            @Parameter(description = "UUID v4 do pedido")
            @PathVariable
            @Size(min = 36, max = 36, message = "ID deve ter exatamente 36 caracteres")
            @Pattern(regexp = UUID_V4_REGEXP, message = "ID deve ser um UUID v4 válido")
            String id) {
        return getOrderStatusUseCase.execute(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
