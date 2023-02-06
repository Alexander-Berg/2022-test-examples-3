package ru.yandex.market.sc.internal.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundInfoRepository;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.movementCourier;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
public class FFApiControllerPutInboundTest {

    private static final String INBOUND_WITH_COURIER_TEMPLATE =
            "put_inbound_dropoff/ff_putInbound_addCourier_sample.xml";

    private static final String INBOUND_WITHOUT_COURIER_TEMPLATE =
            "put_inbound_dropoff/ff_putInbound_createInbound_withoutCourierNode_sample.xml";

    private static final String EXTERNAL_ID = "my_ext_id_123";
    private static final String COURIER_YANDEX_ID = "curYaId";
    private static final Long COURIER_UID = null;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;

    @Autowired
    CourierRepository courierRepository;

    @Autowired
    InboundInfoRepository inboundInfoRepository;

    private SortingCenter sortingCenter;
    private Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
    }

    @Test
    void ffApiRequest() {
        String body = String.format(fileContent("ff_putInbound.xml"), sortingCenter.getToken(), EXTERNAL_ID, sortingCenter.getId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 3, 20, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 4, 21, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo("106");
        assertThat(inbound.getComment()).isEqualTo("comment about inbound");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(inbound.getTransportationId()).isEqualTo("TM-123123");

        //update inbound
        body = String.format(fileContent("ff_putInbound_2.xml"), sortingCenter.getToken(), EXTERNAL_ID, sortingCenter.getId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 1, 20, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 1, 21, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo("777");
        assertThat(inbound.getComment()).isEqualTo("another comment");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(inbound.isConfirmed()).isEqualTo(true);
        assertThat(inbound.getTransportationId()).isEqualTo("TM-123123");
    }

    @Test
    void ffApiRequestXdocWithRealSupplierName() {
        String body = String.format(fileContent("ff_putInbound_3.xml"), sortingCenter.getToken(), EXTERNAL_ID, sortingCenter.getId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 3, 20, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 4, 21, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo("106");
        assertThat(inbound.getComment()).isEqualTo("comment about inbound");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(Objects.requireNonNull(inbound.getInboundInfo()).getRealSupplierName())
                .isEqualTo("test-supplier");
    }


    @Test
    void ffApiRequestWithEmptyRestrictedData() {
        String body = String.format(fileContent("ff_putInbound_empty.xml"), sortingCenter.getToken(), EXTERNAL_ID, sortingCenter.getId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 3, 20, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 4, 21, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo("106");
        assertThat(inbound.getComment()).isEqualTo("comment about inbound");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);

        //update inbound
        body = String.format(fileContent("ff_putInbound_2.xml"), sortingCenter.getToken(), EXTERNAL_ID, sortingCenter.getId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 1, 20, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 1, 21, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo("777");
        assertThat(inbound.getComment()).isEqualTo("another comment");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(inbound.isConfirmed()).isEqualTo(true);
    }

    @Test
    void movementCourierCreated() {
        var movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(0);

        inboundUpdatedWithCourierWithUid();

        movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(1);

        MovementCourier movementCourier = movementCouriers.get(0);
        assertThat(movementCourier.getUid()).isEqualTo(COURIER_UID);
        assertThat(movementCourier.getExternalId()).isEqualTo(COURIER_YANDEX_ID);
        assertThat(movementCourier.getLegalEntityName()).isEqualTo("Кроссдоков Дропшип");
        assertThat(movementCourier.getCarNumber()).isEqualTo("А000МР777");

        List<Courier> couriers = courierRepository.findAll();
        //no need for courier if uid is not present
        assertThat(couriers).hasSize(0);
    }

    @Test
    void movementCourierNotCreatedWhenItsPresent() {
        testFactory.storedMovementCourier(movementCourier(COURIER_YANDEX_ID, "name", "legalName", "А000МР777", null));

        var movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(1);

        inboundUpdatedWithCourierWithUid();

        movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(1);

        MovementCourier movementCourier = movementCouriers.get(0);

        assertThat(movementCourier.getUid()).isEqualTo(COURIER_UID);
        assertThat(movementCourier.getExternalId()).isEqualTo(COURIER_YANDEX_ID);

        assertThat(movementCourier.getLegalEntityName()).isEqualTo("name");
        assertThat(movementCourier.getCarNumber()).isEqualTo("А000МР777");

        List<Courier> couriers = courierRepository.findAll();
        //no need for courier if uid is not present
        assertThat(couriers).hasSize(0);
    }

    @Test
    void inboundUpdatedWithCourierWithUid() {
        inboundCreatedWithoutCourier();

        // Updating existing inbound by assigning a courier (inserting courier data)
        var body = getInboundRequest(INBOUND_WITH_COURIER_TEMPLATE, EXTERNAL_ID, COURIER_YANDEX_ID, COURIER_UID);
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);

        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 3, 20, 12, 0, 0, 0,
                DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 4, 21, 12, 0, 0, 0,
                DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo(COURIER_YANDEX_ID);
        assertThat(inbound.getMovementCourier().getUid()).isEqualTo(COURIER_UID);

        assertThat(inbound.getComment()).isEqualTo("comment about inbound");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
    }

    @Test
    void inboundCreatedWithoutCourier() {
        // Inbound create request without courier
        String body = getInboundRequest(INBOUND_WITHOUT_COURIER_TEMPLATE, EXTERNAL_ID);
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);

        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 3, 20, 12, 0, 0, 0,
                DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 4, 21, 12, 0, 0, 0,
                DateTimeUtil.DEFAULT_ZONE_ID));
        Assertions.assertNull(inbound.getMovementCourier());
        assertThat(inbound.getComment()).isEqualTo("comment about inbound");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
    }

    private String getInboundRequest(String template, String warehouseYandexId, String courierYandexId,
                                     Long courierUid) {
        return String.format(
                fileContent(template),
                sortingCenter.getToken(),
                warehouseYandexId,
                warehouse.getYandexId(),
                courierYandexId, // courier yandex id = external id
                courierUid // courier car description = uid (temporary hack)
        );
    }

    private String getInboundRequest(String template, String warehouseYandexId) {
        return getInboundRequest(template, warehouseYandexId, null, null);
    }
}
