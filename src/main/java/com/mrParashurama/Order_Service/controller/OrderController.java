package com.mrParashurama.Order_Service.controller;

import com.mrParashurama.Order_Service.dto.OrderRequest;
import com.mrParashurama.Order_Service.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/con")
    public String Connect(){
        return orderService.Connect();
    }

    @GetMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory",fallbackMethod = "fallbackPlaceOrder")
    @TimeLimiter(name = "inventory")
    @Retry(name = "inventory")
    public CompletableFuture<String> placeOrder(@RequestBody OrderRequest orderRequest){
        return CompletableFuture.supplyAsync(()-> orderService.placeOrder(orderRequest));
    }
    public CompletableFuture<String> fallbackPlaceOrder(OrderRequest orderRequest,RuntimeException runtimeException){
        return  CompletableFuture.supplyAsync(() -> "Oops! Something went wrong, please order after sometime! ");
    }
}
