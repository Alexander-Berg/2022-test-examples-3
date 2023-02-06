package ru.yandex.market.sc.internal.domain.order;

import java.io.File;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.sc.core.domain.c2c.C2cBoxSizeClass;
import ru.yandex.market.sc.core.domain.c2c.C2cBoxSizeClassRepository;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.OrderScRequest;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.partner.order.PartnerDeliveryOrderRepository;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptReturnedOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.UserCommandService;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.test.TestFactory.CourierWithDs;
import ru.yandex.market.sc.internal.config.WwClientConfiguration;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryBoxDto;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderAction;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderDto;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderVerificationDto;
import ru.yandex.market.sc.internal.controller.mapper.PartnerDeliveryOrderDtoMapper;
import ru.yandex.market.sc.internal.domain.report.PartnerReportService;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus.AWAITING_RECEIVE;
import static ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus.CANCELLED;
import static ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus.DISPATCHED;
import static ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus.READY_TO_RETURN;
import static ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus.RECEIVED;
import static ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus.RETURNED;
import static ru.yandex.market.sc.internal.controller.mapper.PartnerDeliveryOrderDtoMapper.SORTING_CENTER_LEGAL_NAME_PLACEHOLDER;
import static ru.yandex.market.sc.internal.controller.mapper.PartnerDeliveryOrderDtoMapper.SORTING_CENTER_READABLE_NAME_PLACEHOLDER;

@EmbeddedDbIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerDeliveryOrderServiceTest {

    private static final long DELIVERY_SERVICE_UID_START = 1100000000000000L;

    private static final String SENDER_NAME_STUB = "Фамилия Имя";
    private static final String SENDER_PHONE_STUB = "+7 (999) 123-45-67";

    private static final String BOX_TYPE_NAME_STUB = "S";
    private static final String BOX_ICON_URL_STUB = "https://avatars.mds.yandex.net/get-logistics_photo/" +
            "6255253/2a000001821c794fdf27b66705a1a1b8a9d6/orig";
    private static final String RETURN_CELL_NAME = "RET C2C";

    private final Clock clock;
    private final WwClient wwClient;
    private final TestFactory testFactory;
    private final ScanService scanService;
    private final UserCommandService userCommandService;
    private final OrderCommandService orderCommandService;
    private final PartnerReportService partnerReportService;
    private final C2cBoxSizeClassRepository c2cBoxSizeClassRepository;
    private final PartnerDeliveryOrderService partnerDeliveryOrderService;
    private final PartnerDeliveryOrderRepository partnerDeliveryOrderRepository;
    private final TransactionTemplate transactionTemplate;


    private SortingCenter sortingCenter;
    private CourierWithDs courierWithDs;

    @BeforeEach
    void setup() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);
        courierWithDs = testFactory.magistralCourier();
        c2cBoxSizeClassRepository.save(C2cBoxSizeClass.builder()
                .displayType(BOX_TYPE_NAME_STUB)
                .marketType(BOX_TYPE_NAME_STUB)
                .width(121)
                .height(122)
                .length(123)
                .weight(BigDecimal.valueOf(121.12))
                .iconUrl(BOX_ICON_URL_STUB)
                .build());
    }

    @Test
    void testGetOrders() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .deliveryService(courierWithDs.deliveryService())
                        .build())
                .get();

        var orders = partnerDeliveryOrderService.getOrders(Pageable.unpaged(), sortingCenter, null, null, null, null);
        assertThat(orders.getContent()).isEqualTo(List.of(
                PartnerDeliveryOrderDto.builder()
                        .id(order.getId())
                        .externalId(order.getExternalId())
                        .status(AWAITING_RECEIVE)
                        .senderName(SENDER_NAME_STUB)
                        .senderPhone(SENDER_PHONE_STUB)
                        .createdAt(LocalDateTime.ofInstant(order.getCreatedAt(), clock.getZone()))
                        .verificationCode(PartnerDeliveryOrderVerificationDto.builder()
                                .accepted(false)
                                .attemptsLeftToVerify(3)
                                .build())
                        .box(PartnerDeliveryBoxDto.builder()
                                .width(121)
                                .height(122)
                                .length(123)
                                .weight(BigDecimal.valueOf(121.12))
                                .iconUrl(BOX_ICON_URL_STUB)
                                .type(BOX_TYPE_NAME_STUB)
                                .build())
                        .actions(List.of(PartnerDeliveryOrderAction.RECEIVE_CHECK))
                        .receiveCell(null)
                        .returnCell(null)
                        .build()
        ));
    }

    @Test
    void testGetOrder() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .deliveryService(courierWithDs.deliveryService())
                        .build())
                .get();

        var returnedOrder = partnerDeliveryOrderService.getOrder(order.getId(), sortingCenter);
        assertThat(returnedOrder).isEqualTo(PartnerDeliveryOrderDto.builder()
                .id(order.getId())
                .externalId(order.getExternalId())
                .status(AWAITING_RECEIVE)
                .senderName(SENDER_NAME_STUB)
                .senderPhone(SENDER_PHONE_STUB)
                .createdAt(LocalDateTime.ofInstant(order.getCreatedAt(), clock.getZone()))
                .verificationCode(PartnerDeliveryOrderVerificationDto.builder()
                        .accepted(false)
                        .attemptsLeftToVerify(3)
                        .build())
                .receiveCell(null)
                .returnCell(null)
                .box(PartnerDeliveryBoxDto.builder()
                        .width(121)
                        .height(122)
                        .length(123)
                        .weight(BigDecimal.valueOf(121.12))
                        .iconUrl(BOX_ICON_URL_STUB)
                        .type(BOX_TYPE_NAME_STUB)
                        .build())
                .actions(List.of(PartnerDeliveryOrderAction.RECEIVE_FINISH, PartnerDeliveryOrderAction.PRINT_LABEL))
                .build()
        );
    }

    @Test
    void testGetOrderWithUnknownBox() {
        c2cBoxSizeClassRepository.deleteAll();

        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .deliveryService(courierWithDs.deliveryService())
                        .build())
                .get();



        var returnedOrder = partnerDeliveryOrderService.getOrder(order.getId(), sortingCenter);
        assertThat(returnedOrder).isEqualTo(PartnerDeliveryOrderDto.builder()
                .id(order.getId())
                .externalId(order.getExternalId())
                .status(AWAITING_RECEIVE)
                .senderName(SENDER_NAME_STUB)
                .senderPhone(SENDER_PHONE_STUB)
                .createdAt(LocalDateTime.ofInstant(order.getCreatedAt(), clock.getZone()))
                .verificationCode(PartnerDeliveryOrderVerificationDto.builder()
                        .accepted(false)
                        .attemptsLeftToVerify(3)
                        .build())
                .receiveCell(null)
                .returnCell(null)
                .box(PartnerDeliveryBoxDto.builder()
                        .width(121)
                        .height(122)
                        .length(123)
                        .weight(BigDecimal.valueOf(121.12))
                        .iconUrl(BOX_ICON_URL_STUB)
                        .type("Н/Д")
                        .build())
                .actions(List.of(PartnerDeliveryOrderAction.RECEIVE_FINISH, PartnerDeliveryOrderAction.PRINT_LABEL))
                .build()
        );
    }

    @Test
    void testGetOrdersFilterByDate() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .build())
                .get();

        assertThat(getOrderIds(
                LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).minusDays(1),
                LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).plusDays(1),
                null,
                null)
        ).containsExactlyInAnyOrder(
                order.getId()
        );

        assertThat(getOrderIds(
                LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).minusDays(2),
                LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).minusDays(1),
                null,
                null)
        ).isEmpty();

        assertThat(getOrderIds(
                LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).plusDays(1),
                LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).plusDays(2),
                null,
                null)
        ).isEmpty();
    }

    @Test
    void testGetOrdersFilterByStatus() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .build())
                .get();

        assertThat(getOrderIds(
                null, null,
                List.of(AWAITING_RECEIVE),
                null)
        ).containsExactlyInAnyOrder(
                order.getId()
        );

        assertThat(getOrderIds(
                null, null,
                List.of(RECEIVED, DISPATCHED, READY_TO_RETURN, RETURNED, CANCELLED),
                null)
        ).isEmpty();
    }

    @Test
    void testGetOrdersFilterByText() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .build())
                .get();

        assertThat(getOrderIds(
                null, null,
                null,
                "Фамил")
        ).containsExactlyInAnyOrder(
                order.getId()
        );

        assertThat(getOrderIds(
                null, null,
                null,
                "123-45")
        ).containsExactlyInAnyOrder(
                order.getId()
        );

        assertThat(getOrderIds(
                null, null,
                null,
                order.getId().toString())
        ).containsExactlyInAnyOrder(
                order.getId()
        );

        assertThat(getOrderIds(
                null, null,
                null,
                "фывфыв")
        ).isEmpty();
    }

    @Test
    void testAcceptOrder() {
        Cell cell = testFactory.storedMagistralCell(sortingCenter, "DROPOFF", CellSubType.DEFAULT,
                courierWithDs.courier().getId());

        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .deliveryService(courierWithDs.deliveryService())
                        .build())
                .get();

        var returnedOrder = partnerDeliveryOrderService.acceptOrder(sortingCenter, order.getId());
        assertThat(returnedOrder).isEqualTo(PartnerDeliveryOrderDto.builder()
                .id(order.getId())
                .externalId(order.getExternalId())
                .status(RECEIVED)
                .senderName(SENDER_NAME_STUB)
                .senderPhone(SENDER_PHONE_STUB)
                .createdAt(LocalDateTime.ofInstant(order.getCreatedAt(), clock.getZone()))
                .verificationCode(PartnerDeliveryOrderVerificationDto.builder()
                        .accepted(false)
                        .attemptsLeftToVerify(3)
                        .build())
                .receiveCell(cell.getScNumber())
                .returnCell(null)
                .box(PartnerDeliveryBoxDto.builder()
                        .width(121)
                        .height(122)
                        .length(123)
                        .weight(BigDecimal.valueOf(121.12))
                        .iconUrl(BOX_ICON_URL_STUB)
                        .type(BOX_TYPE_NAME_STUB)
                        .build())
                .actions(List.of(PartnerDeliveryOrderAction.PRINT_LABEL))
                .build()
        );
    }

    @Test
    void testAcceptOrderTwiceIsIdempotent() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        var returnedOrder1 = partnerDeliveryOrderService.acceptOrder(sortingCenter, order.getId());
        var returnedOrder2 = partnerDeliveryOrderService.acceptOrder(sortingCenter, order.getId());

        assertThat(returnedOrder1).isEqualTo(returnedOrder2);
    }

    @Test
    void testPrintLabel() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        ArgumentCaptor<List<LabelInfo>> labelInfoCaptor = ArgumentCaptor.forClass(List.class);

        partnerReportService.printC2cLabel(sortingCenter, order.getId());
        verify(wwClient).generateLabels(labelInfoCaptor.capture(), any(), any());

        assertThat(labelInfoCaptor.getValue().get(0)).isEqualTo(LabelInfo.builder()
                .platformClientId(PartnerDeliveryOrderDtoMapper.WW_PLATFORM_CLIENT_ID)
                .barcode(order.getExternalId())
                .shipmentDate(LocalDate.EPOCH)
                .address(LabelInfo.AddressInfo.builder()
                        .country("Россия")
                        .federalDistrict("Центральный федеральный округ")
                        .region("Москва и Московская область")
                        .locality("Москва")
                        .subRegion(null)
                        .settlement("")
                        .street("Звёздный")
                        .house("10")
                        .building("")
                        .housing("1")
                        .room("34")
                        .zipCode("129515")
                        .build())
                .seller(LabelInfo.SellerInfo.builder()
                        .readableName(PartnerDeliveryOrderDtoMapper.SENDER_LEGAL_NAME)
                        .legalName(SENDER_NAME_STUB)
                        .build())
                .sortingCenter(LabelInfo.PartnerInfo.builder()
                        .readableName(SORTING_CENTER_READABLE_NAME_PLACEHOLDER)
                        .legalName(SORTING_CENTER_LEGAL_NAME_PLACEHOLDER)
                        .build())
                .deliveryService(LabelInfo.PartnerInfo.builder()
                        .readableName("3PL")
                        .legalName(PartnerDeliveryOrderDtoMapper.DS_LEGAL_NAME)
                        .build())
                .place(LabelInfo.PlaceInfo.builder()
                        .externalId(order.getExternalId())
                        .placeNumber(1)
                        .placesCount(1)
                        .weight(BigDecimal.valueOf(121.12))
                        .build())
                .recipient(LabelInfo.RecipientInfo.builder()
                        .phoneNumber("79199999999")
                        .lastName("Тест")
                        .firstName("Тест Тест")
                        .build())
                .build());
    }

    @Test
    void testLabelInfoNotChangesAfterOrderAccepted() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        long targetScId = order.getCourierId().get() - DELIVERY_SERVICE_UID_START;
        testFactory.storedSortingCenter(targetScId);

        ArgumentCaptor<List<LabelInfo>> labelBeforeAcceptCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<LabelInfo>> labelAfterAcceptCaptor = ArgumentCaptor.forClass(List.class);

        clearInvocations(wwClient);
        partnerReportService.printC2cLabel(sortingCenter, order.getId());
        verify(wwClient).generateLabels(labelBeforeAcceptCaptor.capture(), any(), any());

        partnerDeliveryOrderService.acceptOrder(sortingCenter, order.getId());

        clearInvocations(wwClient);
        partnerReportService.printC2cLabel(sortingCenter, order.getId());
        verify(wwClient).generateLabels(labelAfterAcceptCaptor.capture(), any(), any());

        LabelInfo labelBeforeAccept = labelBeforeAcceptCaptor.getValue().get(0);
        LabelInfo labelAfterAccept = labelAfterAcceptCaptor.getValue().get(0);

        assertThat(labelBeforeAccept).isEqualTo(labelAfterAccept);
    }

    @Test
    void testVerifyCodeSuccess() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        PartnerDeliveryOrderDto verified = partnerDeliveryOrderService.verifyOrder(
                sortingCenter, order.getId(), "00000");
        assertThat(verified.getVerificationCode().getAttemptsLeftToVerify()).isEqualTo(2);
        assertThat(verified.getVerificationCode().isAccepted()).isEqualTo(true);
    }

    @Test
    void testVerificationIsIdempotent() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        partnerDeliveryOrderService.verifyOrder(sortingCenter, order.getId(), "00000");
        PartnerDeliveryOrderDto verified = partnerDeliveryOrderService.verifyOrder(
                sortingCenter, order.getId(), "00000");

        assertThat(verified.getVerificationCode().getAttemptsLeftToVerify()).isEqualTo(2);
        assertThat(verified.getVerificationCode().isAccepted()).isEqualTo(true);
    }

    @Test
    void testVerifyCodeFail() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        PartnerDeliveryOrderDto verified = partnerDeliveryOrderService.verifyOrder(sortingCenter, order.getId(), "1");
        assertThat(verified.getVerificationCode().getAttemptsLeftToVerify()).isEqualTo(2);
        assertThat(verified.getVerificationCode().isAccepted()).isEqualTo(false);
    }

    @Test
    void testVerifyCodeExceedAttempts() {
        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        PartnerDeliveryOrderDto verified = null;
        for (int i = 0; i < 3; i++) {
            verified = partnerDeliveryOrderService.verifyOrder(sortingCenter, order.getId(), "1");
        }

        assertThat(verified.getVerificationCode().getAttemptsLeftToVerify()).isEqualTo(0);
        assertThat(verified.getVerificationCode().isAccepted()).isEqualTo(false);

        assertThatThrownBy(() -> partnerDeliveryOrderService.verifyOrder(sortingCenter, order.getId(), "1"))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void testReturedOrderStatusAndCell() {
        ScOrder order = createReturnedOrder();
        var deliveryOrder = partnerDeliveryOrderService.getOrder(order.getId(), order.getSortingCenter());

        assertThat(deliveryOrder.getStatus()).isEqualTo(READY_TO_RETURN);
        assertThat(deliveryOrder.getReturnCell()).isEqualTo(RETURN_CELL_NAME);
    }

    @Test
    void testDeliverReturnWithPassport() {
        ScOrder order = createReturnedOrder();

        var returned = partnerDeliveryOrderService.deliverReturn(sortingCenter, order.getId(), true);
        assertThat(returned.getStatus()).isEqualTo(RETURNED);
    }

    @Test
    void testDeliverReturnWithCode() {
        ScOrder order = createReturnedOrder();

        partnerDeliveryOrderService.verifyOrder(sortingCenter, order.getId(), "00000");
        var returned = partnerDeliveryOrderService.deliverReturn(sortingCenter, order.getId(), false);
        assertThat(returned.getStatus()).isEqualTo(RETURNED);
    }

    @Test
    void testNotDeliverReturnWithoutVerification() {
        ScOrder order = createReturnedOrder();

        assertThatThrownBy(() -> partnerDeliveryOrderService.deliverReturn(sortingCenter, order.getId(), false))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    private Place getPlaceForOrder(long id) {
        return transactionTemplate.execute(s -> {
            Place p = partnerDeliveryOrderRepository.findById(id).get().getPlace();
            p.getMainPartnerCode();
            return p;
        });
    }

    private ScOrder createReturnedOrder() {
        Cell c2cReturnCell = testFactory.storedCell(sortingCenter, RETURN_CELL_NAME, CellType.RETURN,
                CellSubType.C2C_RETURN);

        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        partnerDeliveryOrderService.acceptOrder(sortingCenter, order.getId());

        User user = userCommandService.findOrCreatePIAdmin(sortingCenter);
        orderCommandService.shipOrder(
                new OrderScRequest(order.getId(), null, user),
                order.getCourierId().get(), null);

        orderCommandService.cancelOrder(order.getId(), "cancel", false, null);

        Place place = getPlaceForOrder(order.getId());

        scanService.acceptReturnedOrder(
                new AcceptReturnedOrderRequestDto(order.getExternalId(), place.getMainPartnerCode(), null),
                new ScContext(user));
        scanService.sortSortable(
                new SortableSortRequestDto(place, c2cReturnCell),
                new ScContext(user));

        return order;
    }

    private List<Long> getOrderIds(LocalDate dateFrom, LocalDate dateTo,
                                   List<PartnerDeliveryOrderStatus> statuses,
                                   String text) {
        return partnerDeliveryOrderService.getOrders(
                        Pageable.unpaged(), sortingCenter,
                        dateFrom, dateTo, statuses, text)
                .getContent()
                .stream()
                .map(PartnerDeliveryOrderDto::getId)
                .collect(Collectors.toList());
    }

    @Test
    @SneakyThrows
    @Disabled("only for local testing")
    void testReallyPrintLabelOnWerewolfTesting() {
        ExternalServiceProperties wwProperties = new ExternalServiceProperties();
        wwProperties.setUrl("https://logistics-ww.tst.vs.market.yandex.net");

        WwClient realWwClient = new WwClientConfiguration().wwClient(wwProperties, mock(TvmClient.class));
        when(wwClient.generateLabels(any(), any(), any()))
                .thenAnswer(a -> realWwClient.generateLabels(a.getArgument(0), a.getArgument(1), a.getArgument(2)));

        ScOrder order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .isC2c(true)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .get();

        byte[] labelData = partnerReportService.printC2cLabel(sortingCenter, order.getId());

        File downloadsFolder = new File(System.getProperty("user.home"), "downloads");
        if (downloadsFolder.exists()) {
            downloadsFolder.mkdir();
        }
        File labelFile = new File(downloadsFolder, "test-label.pdf");
        FileUtils.writeByteArrayToFile(labelFile, labelData);
    }

}
