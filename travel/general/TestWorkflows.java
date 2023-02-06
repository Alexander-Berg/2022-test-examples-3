package ru.yandex.travel.acceptance.orders.invoice.trust;

import java.util.UUID;

import lombok.Getter;

@Getter
public enum TestWorkflows {
    CHECKER("154e61b6-7590-4cb8-be20-6d24d991a8c0", "CHECKER");

    private final UUID id;
    private final String entityType;

    TestWorkflows(String id, String entityType) {
        this.id = UUID.fromString(id);
        this.entityType = entityType;
    }
}
