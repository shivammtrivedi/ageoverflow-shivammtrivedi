package com.ageoverflow.gcas.repository;

import com.ageoverflow.gcas.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence layer for {@link com.ageoverflow.gcas.model.Customer} entities.
 * All CRUD and existence-check operations are provided by {@link JpaRepository}.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}

