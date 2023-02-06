package ru.yandex.market.tpl.core.domain.order.batch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.external.delivery.sc.SortCenterDirectClient;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderBatchRegistryUpdateServiceTest {

    public static final long USER_SHIFT_ID = 1001L;
    public static final String REQUEST_ID = "requestId";
    public static final String BATCH_BARCODE_SINGLE_ORDER_PLACE = "barcode_singleOrderPlace";
    public static final String BATCH_BARCODE_MULTI_ORDER_PLACE = "barcode_multiOrderPlace";
    public static final String BATCH_ORDER_PLACE_BARCODE_1 = "SC_LOT_1_" + RandomStringUtils.randomNumeric(7, 9);
    public static final String BATCH_ORDER_PLACE_BARCODE_2 = "SC_LOT_2_" + RandomStringUtils.randomNumeric(7, 9);
    public static final String BATCH_ORDER_PLACE_BARCODE_3 = "SC_LOT_3_" + RandomStringUtils.randomNumeric(7, 9);
    public static final String ORDER_YANDEX_ID_1 = "oYandexId1";
    public static final String ORDER_YANDEX_ID_2 = "oYandexId2";
    public static final Map<String, Set<OrderBatchesScDto.BoxBatchSc>> BOX_BATCH_SC_MAP =
            Map.of(
                    BATCH_BARCODE_SINGLE_ORDER_PLACE, Set.of(
                            new OrderBatchesScDto.BoxBatchSc(null, ORDER_YANDEX_ID_1)
                    ),
                    BATCH_BARCODE_MULTI_ORDER_PLACE, Set.of(
                            new OrderBatchesScDto.BoxBatchSc(BATCH_ORDER_PLACE_BARCODE_2, ORDER_YANDEX_ID_2),
                            new OrderBatchesScDto.BoxBatchSc(BATCH_ORDER_PLACE_BARCODE_3, ORDER_YANDEX_ID_2)
                    )
            );
    public static final OrderPlaceDto ORDER_PLACE_DTO_1 = OrderPlaceDto.builder()
            .barcode(new OrderPlaceBarcode("145", BATCH_ORDER_PLACE_BARCODE_1))
            .build();
    public static final OrderPlaceDto ORDER_PLACE_DTO_2 = OrderPlaceDto.builder()
            .barcode(new OrderPlaceBarcode("145", BATCH_ORDER_PLACE_BARCODE_2))
            .build();
    public static final OrderPlaceDto ORDER_PLACE_DTO_3 = OrderPlaceDto.builder()
            .barcode(new OrderPlaceBarcode("145", BATCH_ORDER_PLACE_BARCODE_3))
            .build();
    public static List<OrderPlaceDto> ORDER_PLACE_DTO_LIST_1 = Collections.singletonList(ORDER_PLACE_DTO_1);
    public static List<OrderPlaceDto> ORDER_PLACE_DTO_LIST_2 = Arrays.asList(ORDER_PLACE_DTO_2, ORDER_PLACE_DTO_3);
    public static OrderBatchesScDto BATCHES_SC_DTO_ONGOING_STUBBING;
    private final OrderBatchRepository orderBatchRepository;
    private final OrderRepository orderRepository;
    private final OrderBatchRegistryUpdateService orderBatchRegistryUpdateService;
    private final TestDataFactory testDataFactory;
    @MockBean
    private SortCenterDirectClient sortCenterDirectClient;

    @BeforeEach
    public void setUp() {
        buildBatchesScDto();
    }

    @DisplayName("Проверка сохранения одноместного заказа из батча")
    @Test
    public void processPayloadCreateOrderBatchWithSingleOrderPlaceSuccessTest() {
        OrderBatchRegistryUpdatePayload payload = new OrderBatchRegistryUpdatePayload(REQUEST_ID, USER_SHIFT_ID);
        when(sortCenterDirectClient.getBatchRegistry(anyString())).thenReturn(BATCHES_SC_DTO_ONGOING_STUBBING);
        createOrderWithOrderPlace();

        orderBatchRegistryUpdateService.processPayload(payload);

        Optional<OrderBatch> result = orderBatchRepository.findByBarcode(BATCH_BARCODE_SINGLE_ORDER_PLACE);
        assertTrue(result.isPresent());
        OrderBatch batch = result.get();
        assertEquals(batch.getBarcode(), BATCH_BARCODE_SINGLE_ORDER_PLACE);
        assertNotNull(batch.getPlaces());
        assertEquals(batch.getPlaces().size(), 1);
        assertTrue(getOrderPlaceBarcodes(batch).contains(BATCH_ORDER_PLACE_BARCODE_1));
    }

    @DisplayName("Проверка сохранения многоместного заказа из батча")
    @Test
    public void processPayloadCreateOrderBatchWithMultiOrderPlaceSuccessTest() {
        OrderBatchRegistryUpdatePayload payload = new OrderBatchRegistryUpdatePayload(REQUEST_ID, USER_SHIFT_ID);
        when(sortCenterDirectClient.getBatchRegistry(anyString())).thenReturn(BATCHES_SC_DTO_ONGOING_STUBBING);
        createOrderWithOrderPlace();

        orderBatchRegistryUpdateService.processPayload(payload);

        Optional<OrderBatch> result = orderBatchRepository.findByBarcode(BATCH_BARCODE_MULTI_ORDER_PLACE);
        assertTrue(result.isPresent());
        OrderBatch batch = result.get();
        assertEquals(batch.getBarcode(), BATCH_BARCODE_MULTI_ORDER_PLACE);
        assertNotNull(batch.getPlaces());
        assertEquals(batch.getPlaces().size(), 2);
        assertTrue(getOrderPlaceBarcodes(batch).containsAll(
                Set.of(
                        BATCH_ORDER_PLACE_BARCODE_2,
                        BATCH_ORDER_PLACE_BARCODE_3)
                )
        );
    }

    private void createOrderWithOrderPlace() {
        Order orderWithSingleOrderPlace = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(ORDER_YANDEX_ID_1)
                        .places(ORDER_PLACE_DTO_LIST_1)
                        .build());
        orderRepository.save(orderWithSingleOrderPlace);
        Order orderWithMultiOrderPlace = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(ORDER_YANDEX_ID_2)
                        .places(ORDER_PLACE_DTO_LIST_2)
                        .build());
        orderRepository.save(orderWithMultiOrderPlace);
    }

    private void buildBatchesScDto() {
        BATCHES_SC_DTO_ONGOING_STUBBING =
                OrderBatchesScDto.builder()
                        .batches(List.of(
                                OrderBatchesScDto.Batch.builder()
                                        .barcode(BATCH_BARCODE_SINGLE_ORDER_PLACE)
                                        .boxes(new ArrayList<>(BOX_BATCH_SC_MAP.get(BATCH_BARCODE_SINGLE_ORDER_PLACE)))
                                        .build(),
                                OrderBatchesScDto.Batch.builder()
                                        .barcode(BATCH_BARCODE_MULTI_ORDER_PLACE)
                                        .boxes(new ArrayList<>(BOX_BATCH_SC_MAP.get(BATCH_BARCODE_MULTI_ORDER_PLACE)))
                                        .build()
                                )
                        )
                        .build();
    }

    private Set<String> getOrderPlaceBarcodes(OrderBatch batch) {
        return batch.getPlaces()
                .stream()
                .map(OrderPlace::getBarcode)
                .filter(Objects::nonNull)
                .map(OrderPlaceBarcode::getBarcode)
                .collect(Collectors.toSet());
    }
}
