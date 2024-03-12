package com.mrParashurama.Order_Service.service;

import com.mrParashurama.Order_Service.dto.InventoryResponse;
import com.mrParashurama.Order_Service.dto.OrderLineItemDto;
import com.mrParashurama.Order_Service.dto.OrderRequest;
import com.mrParashurama.Order_Service.event.OrderPlacedEvent;
import com.mrParashurama.Order_Service.feign.FeignInterface;
import com.mrParashurama.Order_Service.model.Order;
import com.mrParashurama.Order_Service.model.OrderLineItem;
import com.mrParashurama.Order_Service.repository.OrderRepository;
import jakarta.persistence.Query;
import jakarta.ws.rs.QueryParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.tools.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final WebClient.Builder webClientBuilder;
    private  final FeignInterface feignInterface;


    private final OrderRepository orderRepository;
    private  final KafkaTemplate<String, OrderPlacedEvent> template;
    public String placeOrder(OrderRequest orderRequest){
        Order order=new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        log.info("order OrderNumber : {}",order.getOrderNumber());
        List<OrderLineItem> orderLineItemList=orderRequest.getOrderLineItemDtos().stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemList(orderLineItemList);
        log.info("order line Items: {}",orderLineItemList);
        //call the inventory service and place the order if product is in stock
      List<String>skuCodes= order.getOrderLineItemList().stream().map(orderLineItem ->orderLineItem.getSkuCode()).collect(Collectors.toList());
        log.info("order list : {}",skuCodes);

      try {
          log.info("please wait connecting to inventory serivce");
//          InventoryResponse inventoryResponseArray[] = webClientBuilder.build().get()//inventory-service
//                  .uri("http://inventory-service/api/inventory",
//                          uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
//                  .retrieve()
//                  .bodyToMono(InventoryResponse[].class)
//                  .block();
//
//
//          Boolean allProductsInStock=Arrays.stream(inventoryResponseArray).allMatch(inventoryResponse -> inventoryResponse.isInStock());
          List<InventoryResponse> inventoryResponses=feignInterface.isInStock(skuCodes);
          Boolean allProductsInStock=inventoryResponses.stream().allMatch(inventoryResponse -> inventoryResponse.isInStock());


          if(allProductsInStock){
              orderRepository.save(order);
              log.info("connection is established");
             //kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
             CompletableFuture<SendResult<String,OrderPlacedEvent>> future= template.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
             future.whenComplete((result,ex) ->{
                 if(ex==null){
                     log.info("Sent message = [{}] with offset =[{}]",order.getOrderNumber(),result.getRecordMetadata().offset());
                 }else{
                     log.info("Unable to Send Message = [{}] due to = [{}]",order.getOrderNumber(),ex.toString());
                 }
             });

              return  "Order placed Successfully";
          }else{
              throw new IllegalArgumentException("product Not in Stock");
          }
      }catch (Exception e){
          throw new IllegalArgumentException("connection is lost "+e.toString());
      }



    }

    private OrderLineItem mapToDto(OrderLineItemDto orderLineItemDto) {
        OrderLineItem orderLineItem=new OrderLineItem();
        orderLineItem.setPrice(orderLineItemDto.getPrice());
        orderLineItem.setQuantity(orderLineItemDto.getQuantity());
        orderLineItem.setSkuCode(orderLineItemDto.getSkuCode());
        return orderLineItem;
    }

    public String Connect() {
        return feignInterface.Connect();
    }
}