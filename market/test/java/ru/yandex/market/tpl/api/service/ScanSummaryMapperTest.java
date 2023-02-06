package ru.yandex.market.tpl.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.OrderBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskSummary;
import ru.yandex.market.tpl.api.model.task.PlaceToAccept;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;

import static org.assertj.core.api.Assertions.assertThat;

class ScanSummaryMapperTest {

    @Test
    void getOrderScanTaskSummary() {
        Set<PlaceToAccept> ordersToAccept = Set.of(
                PlaceToAccept.builder()
                        .orderExternalId("1")
                        .barcode("1-1")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("2")
                        .barcode("2-1")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("2")
                        .barcode("2-2")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("2")
                        .barcode("2-3")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("3")
                        .barcode("3-1")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("3")
                        .barcode("3-2")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("4")
                        .barcode("4-1")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("4")
                        .barcode("4-2")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("5")
                        .barcode("5-1")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("5")
                        .barcode("5-2")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("6")
                        .barcode("6-1")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("6")
                        .barcode("6-2")
                        .build(),
                PlaceToAccept.builder()
                        .orderExternalId("8")
                        .barcode("8-1")
                        .optional(true)
                        .build()
        );
        ScanSummaryMapper mapper = new ScanSummaryMapper();
        Set<OrderBatchDto> batchesToScan = Set.of(
                OrderBatchDto.builder()
                        .barcode("batch-1")
                        .orders(Set.of(
                                OrderDto.builder()
                                        .externalOrderId("1")
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode("1-1")
                                                        .batchBarcode("batch-1")
                                                        .build()
                                        ))
                                        .build(),
                                OrderDto.builder()
                                        .externalOrderId("2")
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode("2-1")
                                                        .batchBarcode("batch-1")
                                                        .build(),
                                                PlaceDto.builder()
                                                        .barcode("2-2")
                                                        .batchBarcode("batch-1")
                                                        .build()
                                        ))
                                        .build(),
                                OrderDto.builder()
                                        .externalOrderId("3")
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode("3-1")
                                                        .batchBarcode("batch-1")
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build(),
                OrderBatchDto.builder()
                        .barcode("batch-2")
                        .orders(Set.of(
                                OrderDto.builder()
                                        .externalOrderId("3")
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode("3-2")
                                                        .batchBarcode("batch-2")
                                                        .build()
                                        ))
                                        .build(),
                                OrderDto.builder()
                                        .externalOrderId("4")
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode("4-1")
                                                        .batchBarcode("batch-2")
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
        OrderScanTaskRequestDto requestFromFrontend = OrderScanTaskRequestDto.builder()
                .scannedBatches(Set.of("batch-1"))
                .scannedOutsidePlaces(Set.of(
                        ScannedPlaceDto.builder()
                                .orderExternalId("2")
                                .barcode("2-2")
                                .build(),
                        ScannedPlaceDto.builder()
                                .orderExternalId("2")
                                .barcode("2-3")
                                .build(),
                        ScannedPlaceDto.builder()
                                .orderExternalId("4")
                                .barcode("4-2")
                                .build(),
                        ScannedPlaceDto.builder()
                                .orderExternalId("5")
                                .barcode("5-1")
                                .build(),
                        ScannedPlaceDto.builder()
                                .orderExternalId("7")
                                .barcode("7-1")
                                .build()
                ))
                .build();
        OrderScanTaskSummary actualSummary = mapper.getOrderScanTaskSummary(
                requestFromFrontend,
                batchesToScan,
                ordersToAccept
        );
        OrderScanTaskSummary expectedSummary = OrderScanTaskSummary.builder()
                .ordersNotAccepted(Set.of("6"))
                .orderExtIdsFullyAccepted(Set.of("1", "2", "7"))
                .placesRemovedFromBatches(Set.of(
                        ScannedPlaceDto.builder()
                                .orderExternalId("2")
                                .barcode("2-2")
                                .build()
                ))
                .partiallyAcceptedOrderExtIdToNotScannedPlaces(Map.of(
                        "3", Set.of("3-2"),
                        "4", Set.of("4-1"),
                        "5", Set.of("5-2")
                ))
                .build();

        assertThat(actualSummary).isEqualTo(expectedSummary);
    }
}
