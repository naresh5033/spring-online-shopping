package com.programmingtechie.orderservice.repository;

import com.programmingtechie.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
// this is Interface                //the prim key of type long
public interface OrderRepository extends JpaRepository<Order, Long> {
} //finally lets inject this repo into the order service 
