package ru.yandex.market.sc.internal.ff.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.FakeOrderType;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.order.sender_verification.OrderSenderVerificationRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehousePropertySource;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FFApiV2CreateOrderProcessorTest {

    private static final long EXTERNAL_ORDER_ID = 200L;
    private static final String DEFAULT_URL_PATH = "/v2/fulfillment/query-gateway";

    private final TestFactory testFactory;
    private final ScOrderRepository scOrderRepository;
    private final OrderSenderVerificationRepository orderSenderVerificationRepository;
    private final WarehousePropertySource warehousePropertySource;
    private final MockMvc mockMvc;

    private static Set<String> getUrlPaths() {
        return Set.of(DEFAULT_URL_PATH, "/v2/fulfillment/query-gateway/createOrder");
    }

    @ParameterizedTest
    @MethodSource("getUrlPaths")
    void createOrder(String urlPath) {
        var warehouse = testFactory.storedWarehouse();
        var sortingCenterPartner = testFactory.storedSortingCenterPartner();
        var deliveryService = testFactory.storedDeliveryService("1", true);

        var sortingCenter = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .build());

        createOrder(sortingCenterPartner.getToken(), sortingCenter.getId(), warehouse.getYandexId(), urlPath);

        var actual = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, String.valueOf(EXTERNAL_ORDER_ID));

        assertThat(actual).isPresent();
        assertThat(actual.get().getExternalId()).isEqualTo(String.valueOf(EXTERNAL_ORDER_ID));
        assertThat(actual.get().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
        assertThat(actual.get().getShipmentDate()).isEqualTo(LocalDate.parse("2020-12-21"));
    }

    @ParameterizedTest
    @MethodSource("getUrlPaths")
    void createOrderUseShipmentDateTime(String urlPath) {
        var warehouse = testFactory.storedWarehouse();
        var sortingCenterPartner = testFactory.storedSortingCenterPartner();

        var sortingCenter = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .build());
        createOrderWithShipmentDateTime(sortingCenterPartner.getToken(), sortingCenter.getId(),
                warehouse.getYandexId(), urlPath);

        var actual = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, String.valueOf(EXTERNAL_ORDER_ID));

        assertThat(actual).isPresent();
        assertThat(actual.get().getShipmentDate()).isEqualTo(LocalDate.parse("2020-12-22"));
        assertThat(actual.get().getShipmentDateTime()).isEqualTo(LocalDateTime.parse("2020-12-22T11:59:00"));
        assertThat(actual.get().getIncomingRouteDate()).isEqualTo(LocalDate.parse("2020-12-22"));
        assertThat(actual.get().getOutgoingRouteDate()).isEqualTo(LocalDate.parse("2020-12-22"));
    }

    @SneakyThrows
    private void createOrder(String token, long sortingCenterId, String warehouseId, String urlPath) {
        String createXml = fileContent("ff_create_order_v2.xml");
        ffApiRequest(String.format(createXml,
                        token, EXTERNAL_ORDER_ID, sortingCenterId, 1L, warehouseId, warehouseId, warehouseId,
                        warehouseId),
                urlPath);
    }

    @SneakyThrows
    private void createOrderWithShipmentDateTime(String token, long sortingCenterId, String warehouseId,
                                                 String urlPath) {
        String createXml = fileContent("ff_create_order_shipment_date_time_v2.xml");
        ffApiRequest(String.format(createXml,
                        token, EXTERNAL_ORDER_ID, sortingCenterId, 1L, warehouseId, warehouseId, warehouseId,
                        warehouseId),
                urlPath);
    }

    @SneakyThrows
    private void ffApiRequest(String body, String urlPath) {
        mockMvc.perform(
                        MockMvcRequestBuilders.post(urlPath)
                                .contentType(MediaType.TEXT_XML)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(xpath("/root/requestState/isError").string("false"));
    }

    @Test
    @DisplayName("Отмена заказа для СЦ на возвратном потоке")
    void createAndCancelOrderOnReturnFlow() {
        var warehouse = testFactory.storedWarehouse();
        var sortingCenterPartner = testFactory.storedSortingCenterPartner();
        var sortingCenter = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .build());
        var deliveryService = testFactory.storedDeliveryService("1", true);

        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.ORDER_CANCEL_ENABLED, "true");

        createOrder(sortingCenterPartner.getToken(), sortingCenter.getId(), warehouse.getYandexId(),
                DEFAULT_URL_PATH);

        var order = scOrderRepository.findBySortingCenterAndExternalId(
                        sortingCenter, String.valueOf(EXTERNAL_ORDER_ID))
                .orElseThrow();

        assertThat(order.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
    }

    @Test
    void createOrderC2C() {
        var warehouse = testFactory.storedWarehouse();
        var sortingCenterPartner = testFactory.storedSortingCenterPartner();
        var sortingCenter = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .build());

        assertThat(warehousePropertySource.warehouseSupportedCellSubtypes(warehouse.getYandexId()))
                .doesNotContain(CellSubType.C2C_RETURN);

        createOrderC2C(
                sortingCenterPartner.getToken(),
                sortingCenter.getId(),
                warehouse.getYandexId(),
                DEFAULT_URL_PATH
        );

        var actual = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, String.valueOf(EXTERNAL_ORDER_ID));

        assertThat(actual).isPresent();
        assertThat(actual.get().getExternalId()).isEqualTo(String.valueOf(EXTERNAL_ORDER_ID));
        assertThat(actual.get().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
        assertThat(actual.get().getFakeOrderType()).isEqualTo(FakeOrderType.C2C_ORDER);

        var senderVerification = orderSenderVerificationRepository.findByOrderIdAndOrderSortingCenter(
                actual.get().getId(), actual.get().getSortingCenter());

        assertThat(senderVerification).isNotNull();
        assertThat(senderVerification.getAttemptsMade()).isEqualTo((short) 0);
        assertThat(senderVerification.isAccepted()).isFalse();
        assertThat(senderVerification.getCode()).isEqualTo("12345");
        assertThat(senderVerification.getSenderName()).isEqualTo("Фамилия Имя");
        assertThat(senderVerification.getSenderPhone()).isEqualTo("+7 (987) 123-45-67");

        assertThat(warehousePropertySource.warehouseSupportedCellSubtypes(warehouse.getYandexId()))
                .contains(CellSubType.C2C_RETURN);
    }

    @SneakyThrows
    private void createOrderC2C(String token, long sortingCenterId, String warehouseId, String urlPath) {
        String createXml = fileContent("ff_create_order_c2c.xml");
        ffApiRequest(String.format(createXml,
                        token, EXTERNAL_ORDER_ID, sortingCenterId, 1L, warehouseId, warehouseId, warehouseId,
                        warehouseId),
                urlPath);
    }

}
