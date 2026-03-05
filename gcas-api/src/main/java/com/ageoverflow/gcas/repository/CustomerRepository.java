package com.ageoverflow.gcas.repository;

import com.ageoverflow.gcas.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}

