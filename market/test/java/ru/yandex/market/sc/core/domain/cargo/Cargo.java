package ru.yandex.market.sc.core.domain.cargo;

public record Cargo(String segmentUuid,
                         String cargoUnitId,
                         String placeBarcode,
                         String warehouseReturnYandexId,
                         String orderBarcode) {
    }
