package ru.yandex.market.tpl.core.domain.sc.batch;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.outbound.dto.BatchRegistryDto;
import ru.yandex.market.tpl.api.model.outbound.dto.BoxesBatchRegistryDto;
import ru.yandex.market.tpl.api.model.outbound.dto.IntervalBatchRegistryDto;
import ru.yandex.market.tpl.api.model.outbound.dto.LogisticPointsBatchRegistryDto;
import ru.yandex.market.tpl.core.domain.sc.batch_update.model.BatchUpdateInternalCourierDto;
import ru.yandex.market.tpl.core.domain.sc.batch_update.model.BatchUpdateOrdersRequest;
import ru.yandex.market.tpl.core.util.TplCoreTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchUpdateOrdersRequestMapperTest {

    public static final String VALID_BATCH_REGISTRY_ID = "tpl_12342353";
    public static final Long VALID_COURIER_ID = 123456L;
    public static final int LOGISTIC_POINTS_COUNT = 2;
    public static final int BOXES_COUNT = 6;
    public static final List<BatchUpdateInternalCourierDto> BATCH_UPDATE_INTERNAL_COURIER_LIST =
            Collections.singletonList(
                    BatchUpdateInternalCourierDto.builder()
                            .id(1L)
                            .name("Name")
                            .companyName("companyName")
                            .phone("88005553535")
                            .carNumber("896")
                            .carDescription("car")
                            .build()
            );
    public static final IntervalBatchRegistryDto INTERVAL_BATCH_REGISTRY = new IntervalBatchRegistryDto(
            OffsetDateTime.parse("2021-03-23T07:00:00Z"),
            OffsetDateTime.parse("2021-03-23T07:00:00Z")
    );
    public static final long SORTING_CENTER_ID = 12342353L;
    public static final LocalDate SHIPMENT_DATE = LocalDate.of(2046, 1, 1);
    public static final long TOTAL_ORDERS = 2L;
    public static final long NEXT_CURSOR_MARK = 3L;
    public static final String TOKEN = "token";
    public static final Map<Long, List<String>> EXTERNAL_ID_LIST_BY_COURIER = Map.of(
            1L, Arrays.asList("extId1", "extId2")
    );
    public static final List<LogisticPointsBatchRegistryDto> LOGISTIC_POINTS = Arrays.asList(
            new LogisticPointsBatchRegistryDto(1L, "name", "address"),
            new LogisticPointsBatchRegistryDto(2L, "name", "address")
    );
    public static final List<BoxesBatchRegistryDto> BOXES = Arrays.asList(
            new BoxesBatchRegistryDto("orderYaId1", 1L),
            new BoxesBatchRegistryDto("orderYaId2", 2L)
    );

    @Test
    @SneakyThrows
    void batchRegistryMapperTest() {
        BatchRegistryDto batchRegistryDto = TplCoreTestUtils.mapFromResource("/batch/valid_batch_registry.json",
                BatchRegistryDto.class);

        assertNotNull(batchRegistryDto);
        assertEquals(batchRegistryDto.getId(), VALID_BATCH_REGISTRY_ID);
        assertEquals(batchRegistryDto.getCourierUid(), VALID_COURIER_ID);
        assertNotNull(batchRegistryDto.getInterval().getTo());
        assertNotNull(batchRegistryDto.getInterval().getFrom());
        assertNotNull(batchRegistryDto.getLogisticPoints());
        assertEquals(batchRegistryDto.getLogisticPoints().size(), LOGISTIC_POINTS_COUNT);
        assertTrue(
                batchRegistryDto.getLogisticPoints().stream()
                        .anyMatch(Objects::nonNull)
        );
        assertNotNull(batchRegistryDto.getBoxes());
        assertEquals(batchRegistryDto.getBoxes().size(), BOXES_COUNT);
        assertTrue(
                batchRegistryDto.getBoxes().stream()
                        .anyMatch(Objects::nonNull)
        );
    }

    @Test
    @SneakyThrows
    void batchUpdateOrdersRequestMapperRest() {
        BatchUpdateOrdersRequest batchUpdateOrdersRequest = buildValidBatchOrderRequest();

        var expected = TplCoreTestUtils
                .mapFromResource("/batch/valid_batch_update_order_request.json", BatchUpdateOrdersRequest.class);

        assertNotNull(expected);
        assertEquals(expected, batchUpdateOrdersRequest);
    }

    private BatchUpdateOrdersRequest buildValidBatchOrderRequest() {
        return BatchUpdateOrdersRequest.builder()
                .sortingCenterId(SORTING_CENTER_ID)
                .shipmentDate(SHIPMENT_DATE)
                .courierDtoList(BATCH_UPDATE_INTERNAL_COURIER_LIST)
                .totalOrders(TOTAL_ORDERS)
                .nextCursorMark(NEXT_CURSOR_MARK)
                .externalIdListByCourier(EXTERNAL_ID_LIST_BY_COURIER)
                .token(TOKEN)
                .batchRegistries(
                        Collections.singletonList(
                                BatchRegistryDto.builder()
                                        .id(VALID_BATCH_REGISTRY_ID)
                                        .courierUid(VALID_COURIER_ID)
                                        .interval(INTERVAL_BATCH_REGISTRY)
                                        .logisticPoints(LOGISTIC_POINTS)
                                        .boxes(BOXES)
                                        .build()
                        )
                )
                .build();
    }
}
