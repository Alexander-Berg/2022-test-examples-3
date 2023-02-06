package ru.yandex.market.sc.internal.controller;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.sc.core.test.TestFactory.movementCourier;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

/**
 * Тест процесса приемки заказов от курьеров с дропоффов (мы принимаем заказы от мерчей на пунктов выдачи заказов
 * и везем их в сц) на наших сортировочных центрах.
 */
@ScIntControllerTest
public class FFApiControllerPutInboundDropOffTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;

    private SortingCenter sortingCenter;
    private Warehouse warehouse;

    private static final String EXTERNAL_ID = "my_ext_id_123";
    private static final String COURIER_YANDEX_ID = "curYaId";
    private static final Long COURIER_UID = 18L;

    @Autowired
    InboundRepository inboundRepository;

    @Autowired
    CourierRepository courierRepository;

    private static final String INBOUND_WITH_COURIER_TEMPLATE =
            "put_inbound_dropoff/ff_putInbound_addCourier_sample.xml";

    private static final String INBOUND_WITHOUT_COURIER_TEMPLATE =
            "put_inbound_dropoff/ff_putInbound_createInbound_withoutCourierNode_sample.xml";

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
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
    void courierCreatedWhenUidPresent() {
        inboundUpdatedWithCourierWithUid();

        Courier courier = courierRepository.findByIdOrThrow(COURIER_UID);

        //values from ff_putInbound_addCourier_sample.xml
        assertThat(courier.getId()).isEqualTo(COURIER_UID);
        assertThat(courier.getName()).isEqualTo("Кроссдоков Дропшип");
        assertThat(courier.getCompanyName()).isEqualTo("legalName");
        assertThat(courier.getCarNumber()).isEqualTo("А000МР777");
        assertThat(courier.getPhone()).isEqualTo("+79181234567");
    }

    @Test
    void courierIsNotModifiedWhenUidPresentAndCourierPresent() {
        testFactory.storedEmptyCourier(COURIER_UID, "existing courier");
        inboundUpdatedWithCourierWithUid();

        Courier courier = courierRepository.findByIdOrThrow(COURIER_UID);

        assertThat(courier.getId()).isEqualTo(COURIER_UID);
        assertThat(courier.getName()).isEqualTo("existing courier");
        assertThat(courier.getCompanyName()).isNull();
        assertThat(courier.getCarNumber()).isNull();
        assertThat(courier.getPhone()).isNull();

    }

    @Test
    @Disabled
        // Due to tm external id issue
        //migration from null UIDs to actual ones
    void movementCurrierUidUpdatedAndCourierCreatedWhenMovementCourierWithSameExternalIdPresent() {
        testFactory.storedMovementCourier(movementCourier(
                COURIER_YANDEX_ID, "name", "legalName", null, null
        ));

        var movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(1);

        inboundUpdatedWithCourierWithUid();

        movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(1);
        assertThat(movementCouriers.get(0).getUid()).isEqualTo(COURIER_UID);

        List<Courier> couriers = courierRepository.findAll();
        assertThat(couriers).hasSize(1);
        Courier courier = couriers.get(0);

        //values from ff_putInbound_addCourier_sample.xml
        assertThat(courier.getId()).isEqualTo(COURIER_UID);
        assertThat(courier.getName()).isEqualTo("Кроссдоков Дропшип");
        assertThat(courier.getCompanyName()).isEqualTo("legalName");
        assertThat(courier.getCarNumber()).isEqualTo("А000МР777");
        assertThat(courier.getPhone()).isEqualTo("+79181234567");

    }

    @Test
    @Disabled
        // due to TM external id issue
        //migration from null UIDs to actual ones, when courier already exists
    void movementCourierUidUpdatedWhenCourierAndMovementCourierWithSameExternalIdPresent() {
        testFactory.storedMovementCourier(movementCourier(
                COURIER_YANDEX_ID, "name", "legalName", null, null
        ));
        testFactory.storedCourier(COURIER_UID);

        var movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(1);

        inboundUpdatedWithCourierWithUid();

        movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(1);
        assertThat(movementCouriers.get(0).getUid()).isEqualTo(COURIER_UID);

        List<Courier> couriers = courierRepository.findAll();
        assertThat(couriers).hasSize(1);
        Courier courier = couriers.get(0);

        assertThat(courier.getId()).isEqualTo(COURIER_UID);
    }

    @Test
    void errorNotThrownWhenThereAreSeveralCouriersWithSameExternalId() {
        String otherCourierName = "other name";
        testFactory.storedMovementCourier(movementCourier(
                COURIER_YANDEX_ID, "name", "legalName", "А000МР777", null
        ));
        testFactory.storedMovementCourier(movementCourier(
                COURIER_YANDEX_ID, "name", "legalName", "А000МР777", 111L
        ));
        testFactory.storedMovementCourier(movementCourier(
                COURIER_YANDEX_ID, otherCourierName, "legalName", "А000МР777", COURIER_UID
        ));
        testFactory.storedCourier(COURIER_UID);

        var movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(3);

        inboundUpdatedWithCourierWithoutUid();

        //redundant checks
        movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers).hasSize(3);
        MovementCourier movementCourier =
                movementCouriers.stream().filter(mc -> COURIER_UID.equals(mc.getUid())).findFirst().orElseThrow();
        assertThat(movementCourier.getLegalEntityName()).isEqualTo(otherCourierName);

        List<Courier> couriers = courierRepository.findAll();
        assertThat(couriers).hasSize(1);
        Courier courier = couriers.get(0);

        assertThat(courier.getId()).isEqualTo(COURIER_UID);
    }

    @Test
    void newCourierNotCreatedWhenExternalIdPresent() {
        inboundUpdatedWithCourierWithoutUid();

        List<Courier> couriers = courierRepository.findAll();

        assertThat(couriers).hasSize(0);
    }

    @Test
    void inboundUpdatedWithCourierWithoutUid() {
        Long courierUid = null;

        inboundCreatedWithoutCourier();
        // Updating existing inbound by assigning a courier (inserting courier data)
        var body = getInboundRequest(INBOUND_WITH_COURIER_TEMPLATE, EXTERNAL_ID, COURIER_YANDEX_ID, courierUid);
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);

        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 3, 20, 12, 0, 0, 0,
                DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 4, 21, 12, 0, 0, 0,
                DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo(COURIER_YANDEX_ID);
        assertThat(inbound.getMovementCourier().getUid()).isEqualTo(courierUid);
        assertThat(inbound.getComment()).isEqualTo("comment about inbound");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
    }

    @Test
    void noNewCourierCreatedWhenPutIsSentWithSameUidAndExternalId() {
        // Inbound create request without courier
        String body = getInboundRequest(INBOUND_WITHOUT_COURIER_TEMPLATE, EXTERNAL_ID, COURIER_YANDEX_ID, COURIER_UID);

        assertMovementCouriersCount(0);
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        assertMovementCouriersCount(0);

        // Updating existing inbound by assigning a courier (inserting courier data)
        body = getInboundRequest(INBOUND_WITH_COURIER_TEMPLATE, EXTERNAL_ID, COURIER_YANDEX_ID, COURIER_UID);
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);

        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo(COURIER_YANDEX_ID);
        assertThat(inbound.getMovementCourier().getUid()).isEqualTo(COURIER_UID);
        assertMovementCouriersCount(1);

        // Updating existing inbound by assigning courier with same UID (updating courier data)
        body = getInboundRequest(
                INBOUND_WITH_COURIER_TEMPLATE, EXTERNAL_ID,
                COURIER_YANDEX_ID, COURIER_UID);


        assertCouriersCount(1);

        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);

        List<MovementCourier> movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers.size()).isEqualTo(1);

        // put changes are not supported for Couriers now, so old values should remain
        MovementCourier movementCourier = movementCouriers.get(0);
        assertThat(movementCourier.getExternalId()).isEqualTo(COURIER_YANDEX_ID);
        assertThat(movementCourier.getUid()).isEqualTo(COURIER_UID);

    }

    private void assertCouriersCount(int count) {
        List<MovementCourier> movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers.size()).isEqualTo(count);
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
        return getInboundRequest(template, warehouseYandexId, "213123", 212_85_06L);
    }

    private void assertMovementCouriersCount(int count) {
        List<MovementCourier> movementCouriers = testFactory.getMovementCouriers();
        assertThat(movementCouriers.size()).isEqualTo(count);
    }

    @Test
    @SneakyThrows
    void checkIfPartnerIdIsReturned() {
        // Inbound create request without courier
        String body = getInboundRequest(INBOUND_WITHOUT_COURIER_TEMPLATE, EXTERNAL_ID);
        ResultActions resultActions = ScTestUtils.ffApiSuccessfulCall(mockMvc, body);


        var persistedInbound = testFactory.getInbound(EXTERNAL_ID);
        resultActions.andExpect(xpath("/root/response/inboundId/partnerId")
                .string(persistedInbound.getId().toString()));


        body = getInboundRequest(INBOUND_WITH_COURIER_TEMPLATE, EXTERNAL_ID);
        resultActions = ScTestUtils.ffApiSuccessfulCall(mockMvc, body);

        persistedInbound = testFactory.getInbound(EXTERNAL_ID);
        resultActions.andExpect(xpath("/root/response/inboundId/partnerId")
                .string(persistedInbound.getId().toString()));


        //test if all returned partner ids matched with ones created in database
        for (int i = 0; i < 10; i++) {

            String newExternalId = EXTERNAL_ID + i;
            body = getInboundRequest(INBOUND_WITH_COURIER_TEMPLATE, newExternalId);
            resultActions = ScTestUtils.ffApiSuccessfulCall(mockMvc, body);

            persistedInbound = testFactory.getInbound(newExternalId);
            resultActions.andExpect(xpath("/root/response/inboundId/partnerId")
                    .string(persistedInbound.getId().toString()));

        }

    }

    @Test
    void checkIfInboundRegistryAssigned() {
        // Inbound create request without courier
        String body = getInboundRequest(INBOUND_WITHOUT_COURIER_TEMPLATE, EXTERNAL_ID);
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);

        body = getInboundRequest(INBOUND_WITH_COURIER_TEMPLATE, EXTERNAL_ID);
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);

        var inbound = testFactory.getInbound(EXTERNAL_ID);
        Long inboundId = inbound.getId();

        body = String.format(fileContent("ff_putInbound_registry_special_case.xml"), sortingCenter.getToken(),
                inbound.getExternalId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var registryList = testFactory.getRegistryByInboundId(inbound.getId());
        assertThat(registryList).hasSize(1);
        var registry = registryList.get(0);
        assertThat(registry.getExternalId()).isEqualTo("registry_external_id");
        assertThat(registry.getInbound().getId()).isEqualTo(inboundId);
        assertThat(registry.getType()).isEqualTo(RegistryType.PLANNED);
        List<RegistryOrder> orders = testFactory.getRegistryOrdersByRegistryExternalId(registry.getId());
        assertThat(orders).hasSize(4);
        assertThat(StreamEx.of(orders).filter(item -> item.getExternalId().equals("multiPlace_external_order_id"))
                .toList()).hasSize(2);
        assertThat(StreamEx.of(orders).filter(item -> item.getStatus().equals(InboundRegistryOrderStatus.CREATED))
                .toList()).hasSize(4);
        assertThat(StreamEx.of(orders).filter(item -> item.getPlaceId().equals("place_external_id_1"))
                .toList()).hasSize(1);
        assertThat(StreamEx.of(orders).filter(item -> item.getPlaceId().equals("place_external_id_2"))
                .toList()).hasSize(1);

        var multiPlaceOrderPlace1 = StreamEx.of(orders)
                .findFirst(item -> item.getPlaceId().equals("place_external_id_1")).get();
        var multiPlaceOrderPlace2 = StreamEx.of(orders)
                .findFirst(item -> item.getPlaceId().equals("place_external_id_2")).get();
        assertThat(multiPlaceOrderPlace1.getExternalId()).isEqualTo("multiPlace_external_order_id");
        assertThat(multiPlaceOrderPlace2.getExternalId()).isEqualTo("multiPlace_external_order_id");

        assertThat(testFactory
                .getRegistryById(multiPlaceOrderPlace1.getRegistryId()).getExternalId())
                .isEqualTo("registry_external_id");

        assertThat(testFactory
                .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                .getInbound().getId()).isEqualTo(inboundId);
        assertThat(testFactory
                .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                .getInbound().getId()).isEqualTo(inboundId);
        assertThat(multiPlaceOrderPlace1.getPalletId()).isEqualTo("first_pallet_id");
        assertThat(multiPlaceOrderPlace2.getPalletId()).isEqualTo("first_pallet_id");

        var regularOrder1 = StreamEx.of(orders)
                .findFirst(item -> item.getPlaceId().equals("regular_order_external_id")).get();
        assertThat(regularOrder1.getPlaceId()).isEqualTo("regular_order_external_id");
        assertThat(testFactory
                .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                .getInbound().getId()).isEqualTo(inboundId);
        assertThat(regularOrder1.getPalletId()).isEqualTo("first_pallet_id");
        assertThat(testFactory
                .getRegistryById(regularOrder1.getRegistryId()).getExternalId())
                .isEqualTo("registry_external_id");

        var regularOrder2 = StreamEx.of(orders)
                .findFirst(item -> item.getPlaceId().equals("regular_order_external_id_2")).get();
        assertThat(regularOrder2.getPlaceId()).isEqualTo("regular_order_external_id_2");
        assertThat(testFactory
                .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                .getInbound().getId()).isEqualTo(inboundId);
        assertThat(testFactory
                .getRegistryById(regularOrder2.getRegistryId()).getExternalId())
                .isEqualTo("registry_external_id");
        assertThat(regularOrder2.getPalletId()).isEqualTo("second_pallet_id");
    }

}
