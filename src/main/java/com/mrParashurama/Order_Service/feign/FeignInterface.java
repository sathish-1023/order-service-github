package com.mrParashurama.Order_Service.feign;

import com.mrParashurama.Order_Service.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@FeignClient(value = "inventory-service",path = "/api/inventory")
public interface FeignInterface {
    @GetMapping("/con")
    public String Connect();
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam(value = "skuCode") List<String> skuCode) ;
}
