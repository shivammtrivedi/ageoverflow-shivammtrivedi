package com.ageoverflow.gcas.repository;

import com.ageoverflow.gcas.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Persistence layer for {@link com.ageoverflow.gcas.model.Customer} entities.
 * All CRUD and existence-check operations are provided by {@link JpaRepository}.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Inserts a customer row if one does not already exist for the given ID.
     * Uses ON CONFLICT DO NOTHING so concurrent first-requests for the same
     * customer_id are safe — one thread inserts, the rest silently skip.
     */
    @Modifying
    @Query(value = "INSERT INTO customers (id) VALUES (:id) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("id") UUID id);
}

