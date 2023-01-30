package com.programmingtechie.productservice.repository;

import com.programmingtechie.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

// this product repo extends from mongorepo 
public interface ProductRepository extends MongoRepository<Product, String> {
}
