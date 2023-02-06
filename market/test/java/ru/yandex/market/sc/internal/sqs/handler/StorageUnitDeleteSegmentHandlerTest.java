package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorDto;
import ru.yandex.market.logistics.les.tpl.StorageUnitDeleteSegmentResponseEvent;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderIdResponse;
import ru.yandex.market.sc.core.domain.order.model.OrderReturnType;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.LocationDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.SqsEventFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author: dbryndin
 * @date: 12/28/21
 */
@EmbeddedDbIntTest
public class StorageUnitDeleteSegmentHandlerTest {

    @Autowired
    private StorageUnitDeleteSegmentHandler storageUnitDeleteSegmentHandler;

    @Autowired
    private TestFactory testFactory;

    @Autowired
    Clock clock;

    @Autowired
    private SqsEventFactory sqsEventFactory;

    private SortingCenter sortingCenter;
    @Autowired
    private CargoUnitCreateHandler cargoUnitCreateHandler;
    @Autowired
    private ScOrderRepository scOrderRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OrderCommandService orderCommandService;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
    }


    @Test
    @DisplayName("success удаление сегментов у части заказов")
    public void successDeleteSegment() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        var segmentUuid0 = "segment_uuid_0";
        var segmentUuid1 = "segment_uuid_1";
        var segmentUuid2 = "segment_uuid_2";

        String cargoUnitId0 = "cargoUnitId_0";
        String cargoUnitId1 = "cargoUnitId_1";
        String cargoUnitId2 = "cargoUnitId_2";
        {
            createClientReturn(cargoUnitId0, segmentUuid0);
            createClientReturn(cargoUnitId1, segmentUuid1);
            createClientReturn(cargoUnitId2, segmentUuid2);
            var order0 = scOrderRepository.findByCargoUnitIdAndSegmentUid(cargoUnitId0, segmentUuid0);
            assertNotNull(order0);
            var order1 = scOrderRepository.findByCargoUnitIdAndSegmentUid(cargoUnitId1, segmentUuid1);
            assertNotNull(order1);
            var order2 = scOrderRepository.findByCargoUnitIdAndSegmentUid(cargoUnitId2, segmentUuid2);
            assertNotNull(order2);
        }
        {
            var responsePayload =
                    storageUnitDeleteSegmentHandler.handle(sqsEventFactory.createSegmentDeleteEvent(segmentUuid0,
                            cargoUnitId0));
            List<StorageUnitResponseErrorDto> errors =
                    ((StorageUnitDeleteSegmentResponseEvent) responsePayload).getResult().getErrors();
            assertTrue(errors.isEmpty());
            storageUnitDeleteSegmentHandler.handle(sqsEventFactory.createSegmentDeleteEvent(segmentUuid1,
                    cargoUnitId1));
            var order2 = scOrderRepository.findByCargoUnitIdAndSegmentUid(cargoUnitId2, segmentUuid2);
            assertNotNull(order2);
        }

    }

    @Test
    @DisplayName("fail нельзя удалить сегмент если грузоместо уже прибыло на сц")
    public void failDeleteSegmentCargoUnitAlreadyOnSc() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        var segmentUuid0 = "segment_uuid_0";
        String cargoUnitId0 = "cargoUnitId_0";
        var externalId = createClientReturn(cargoUnitId0, segmentUuid0).getExternalId();
        var order = scOrderRepository.findBySortingCenterAndExternalId(sortingCenter, externalId);
        testFactory.accept(order.get());
        var responsePayload =
                storageUnitDeleteSegmentHandler.handle(sqsEventFactory.createSegmentDeleteEvent(segmentUuid0,
                        cargoUnitId0));
        var errors = ((StorageUnitDeleteSegmentResponseEvent) responsePayload).getResult().getErrors();
        assertFalse(errors.isEmpty(), "must be not empty, but empty");

    }

    private final LocationDto MOCK_WAREHOUSE_LOCATION = LocationDto.builder()
            .country("Россия")
            .region("Москва и Московская область")
            .locality("Котельники")
            .build();

    private OrderIdResponse createClientReturn(String cargoUnitId, String segmentUuid) {
        var fromWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SORTING_CENTER.name())
                .yandexId("123123")
                .logisticPointId("123123")
                .incorporation("ООО фром мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .build();
        var returnWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SORTING_CENTER.name())
                .yandexId("222222")
                .logisticPointId("222222")
                .location(MOCK_WAREHOUSE_LOCATION)
                .incorporation("ООО ретурн мерчант")
                .build();
        return orderCommandService.createReturn(CreateReturnRequest.builder()
                .sortingCenter(sortingCenter)
                .orderBarcode(cargoUnitId + "_" + segmentUuid)
                .returnDate(LocalDate.now())
                .returnWarehouse(returnWarehouse)
                .fromWarehouse(fromWarehouse)
                .segmentUuid(segmentUuid)
                .orderReturnType(OrderReturnType.CLIENT_RETURN)
                .cargoUnitId(cargoUnitId)
                .timeIn(Instant.now(clock))
                .timeOut(Instant.now(clock))
                .build()
        , testFactory.getOrCreateAnyUser(sortingCenter));
    }
}
