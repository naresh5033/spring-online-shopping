package com.programmingtechie.orderservice.service;

import brave.Span;
import brave.Tracer;
import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemsDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.event.OrderPlacedEvent;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItems;
import com.programmingtechie.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service 
@RequiredArgsConstructor //will create ctor during runtime
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository; //here we re injecting the interface (in ctor), and need to create ctor, instead we'll use annotation
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer; // from spring cloud sleuth so that we can add our own SpanId
    //let set the kafka template for our  producer
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    //orderReq from dto
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        //OrderlineItems from model
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto) //we replaced this lambda with method ref
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        // here we re collecting all the skucodes from the order obj
        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList(); // now we can add skucode to the query param in api call

        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup"); // the custom spanId

        try (Tracer.SpanInScope isLookup = tracer.withSpanInScope(inventoryServiceLookup.start())) {

            inventoryServiceLookup.tag("call", "inventory-service");
            // Call Inventory Service thru webclient (sync msg), and place order if product is in
            // stock
            InventoryResponse[] inventoryResponsArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve() // to retrieve the resp
                    .bodyToMono(InventoryResponse[].class) //body to mono from reactive class, to read the resp.. the webclient will parse the [] of inv resp obj and will provide it as a result
                    .block(); // in order to get the sync req 

             // lets verify the products in stock or not       
            boolean allProductsInStock = Arrays.stream(inventoryResponsArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                //save the order in repo and produce the msg and send the notification(order placed event as json msg) via kafka
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));//these 2 args are K & V
                return "Order Placed Successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        } finally {
            inventoryServiceLookup.flush();
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
