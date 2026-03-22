package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.application.dto.OrderRequest;
import com.olivertech.orderservice.application.dto.OrderResponse;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.CreateOrderUseCase;
import com.olivertech.orderservice.domain.port.in.GetOrderStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Endpoints de gestão de pedidos")
@Validated
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderStatusUseCase getOrderStatusUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           GetOrderStatusUseCase getOrderStatusUseCase) {
        this.createOrderUseCase    = createOrderUseCase;
        this.getOrderStatusUseCase = getOrderStatusUseCase;
    }

    @Operation(summary = "Criar pedido",
            description = "Aceita pedido e publica evento Kafka de forma assíncrona.")
    @ApiResponse(responseCode = "202", description = "Pedido aceito")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "503", description = "Kafka temporariamente indisponível")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        return createOrderUseCase.execute(request);
    }

    @Operation(summary = "Consultar status do pedido")
    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatus> getStatus(
            @Parameter(description = "UUID v4 do pedido")
            @PathVariable
            // CORREÇÃO: validação do formato para evitar path traversal e injection
            @Pattern(
                    regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                    message = "ID deve ser um UUID v4 válido"
            )
            String id) {
        return getOrderStatusUseCase.execute(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
