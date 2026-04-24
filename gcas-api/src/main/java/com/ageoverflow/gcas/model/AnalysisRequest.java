package com.ageoverflow.gcas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analysis_requests", indexes = {
        @Index(name = "idx_ar_customer_id",    columnList = "customer_id"),
        @Index(name = "idx_ar_user_id",        columnList = "user_id"),
        @Index(name = "idx_ar_status",         columnList = "status"),
        @Index(name = "idx_ar_created_at",     columnList = "created_at"),
        @Index(name = "idx_ar_primary_gen",    columnList = "primary_generation"),
        @Index(name = "idx_ar_customer_created", columnList = "customer_id, created_at"),
})
@Getter
@Setter
@NoArgsConstructor
public class AnalysisRequest {

    @Id
    private UUID id;

    // Many requests can belong to one customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private boolean urgent;

    // Store enum as string in DB ("PENDING", "SUCCESS", "FAILED") rather than ordinal
    // so DB values remain readable and stable if enum order changes
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Photos are stored in a separate table (request_photos) linked by request_id.
    // Each photo is a base64-encoded string. Using ElementCollection avoids needing
    // a full entity for something that has no identity of its own.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "request_photos", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "photo", columnDefinition = "TEXT")
    private List<String> photos;

    // --- Result fields ---
    // These are all nullable and only populated once the engine has finished processing.
    // Flattened here rather than a separate table to keep queries simple.

    private String checksum;

    // Generation confidence percentages from the engine output
    @Column(name = "gen_silent")
    private Double genSilent;

    @Column(name = "gen_baby_boomers")
    private Double genBabyBoomers;

    @Column(name = "gen_x")
    private Double genX;

    @Column(name = "gen_y")
    private Double genY;

    @Column(name = "gen_z")
    private Double genZ;

    @Column(name = "gen_alpha")
    private Double genAlpha;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_generation")
    private Generation primaryGeneration;

    private Double age;

    // Called automatically by JPA before the entity is first saved.
    // Ensures both timestamps are set at creation time.
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Called automatically by JPA before any subsequent save.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
