package ru.yandex.market.pvz.internal.domain.order;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.LogPickupPointScanRepository;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScan;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScanDetails;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScanType;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultItemParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.partial_delivery.OrderPartialDeliveryOrderInfoDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.partial_delivery.OrderPartialDeliveryPageDto;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow.RETURN;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_2;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderPartialDeliveryPartnerServiceTest {

    private final OrderPartialDeliveryPartnerService orderPartialDeliveryPartnerService;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final TestOrderFactory orderFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestableClock clock;
    private final LogPickupPointScanRepository logPickupPointScanRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @MockBean
    private PersonalExternalService personalExternalService;

    @Test
    void testScanSecondItemInstanceId() {
        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneId.systemDefault());
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = orderFactory.createSimpleFashionOrder(false, pickupPoint);
        orderFactory.receiveOrder(order.getId());

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        OrderDeliveryResultItemParams secondItem = deliveryResult.getItems().get(1);
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );
        orderPartialDeliveryPartnerService.updateItemFlow(
                pickupPointRequestData, order.getId(), null, secondItem.getItemInstanceId(),
                ItemDeliveryFlow.RETURN, ItemDeliveryScanType.SCAN, null
        );

        LogPickupPointScan actual = logPickupPointScanRepository.findAll().get(0);
        LogPickupPointScan expected = LogPickupPointScan.builder()
                .pickupPointId(pickupPoint.getId())
                .logPickupPointScanType(LogPickupPointScanType.REJECT_PARTIAL_DELIVERED_ITEM)
                .scannedAt(Instant.now(clock))
                .uid("1")
                .details(LogPickupPointScanDetails.builder()
                        .itemInstanceId(secondItem.getItemInstanceId())
                        .flow(RETURN)
                        .build())
                .build();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void testPackageSafePackage() {
        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneId.systemDefault());
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order order = orderFactory.createSimpleFashionOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .externalId("6")
                        .fbs(false)
                        .recipientPhone("12345")
                        .recipientEmail("email")
                        .recipientName("Иванов Иван Иванович")
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );
        orderPartialDeliveryPartnerService.scanSafePackage(pickupPointRequestData, order.getId(), BARCODE_1);

        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);
        String phoneNew = "+71112223344";
        String emailNew = "some@mail.ru";
        String forename = "Василий";
        String surname = "Пупкин";
        List<MultiTypeRetrieveResponseItem> responseItems = List.of(
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).id("1234")
                        .value(new CommonType().phone(phoneNew)),
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.EMAIL).id("4321")
                        .value(new CommonType().email(emailNew)),
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.FULL_NAME).id("5678")
                        .value(new CommonType().fullName(new FullName().forename(forename).surname(surname)))
        );
        when(personalExternalService.getMultiTypePersonalByIds(any())).thenReturn(responseItems);

        OrderPartialDeliveryPageDto page = orderPartialDeliveryPartnerService.scanSafePackage(
                pickupPointRequestData, order.getId(), BARCODE_2
        );

        OrderPartialDeliveryOrderInfoDto orderInfo = page.getOrderInfo();
        assertThat(orderInfo.getRecipientPhone()).isEqualTo(phoneNew);
        assertThat(orderInfo.getRecipientName()).isEqualTo(surname + " " + forename);

        List<LogPickupPointScan> actual = logPickupPointScanRepository.findAll();
        List<LogPickupPointScan> expected = new ArrayList<>();
        expected.add(buildLogOrderScan(pickupPoint.getId(), order.getId(), BARCODE_1));
        expected.add(buildLogOrderScan(pickupPoint.getId(), order.getId(), BARCODE_2));

        String[] comparationIgnoredFields = new String[] {"id"};
        assertThat(actual).usingElementComparatorIgnoringFields(comparationIgnoredFields)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private LogPickupPointScan buildLogOrderScan(Long pickupPointId, Long orderId, String barcode) {
        return LogPickupPointScan.builder()
                .pickupPointId(pickupPointId)
                .logPickupPointScanType(LogPickupPointScanType.ADD_SAFE_PACKAGE)
                .scannedAt(Instant.now(clock))
                .uid("1")
                .details(LogPickupPointScanDetails.builder()
                        .orderId(orderId)
                        .barcode(barcode)
                        .build())
                .build();
    }
}
