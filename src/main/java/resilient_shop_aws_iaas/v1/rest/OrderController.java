package resilient_shop_aws_iaas.v1.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resilient_shop_aws_iaas.v1.application.OrderUseCase;
import resilient_shop_aws_iaas.v1.domain.Order;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderUseCase useCase;

    public OrderController(OrderUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Bulk creating orders",
            description = "Create many orders"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Orders created sucessfully"
    )
    public ResponseEntity<Void> create(@RequestBody List<Order> orders) {
        useCase.createOrders(orders);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
