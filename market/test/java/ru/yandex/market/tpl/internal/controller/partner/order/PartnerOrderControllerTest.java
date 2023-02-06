package ru.yandex.market.tpl.internal.controller.partner.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.order.ChequeUrlDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderSenderDto;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderAddressDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDeliveryDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDetailsDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderEventDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerRecipientDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderDto;
import ru.yandex.market.tpl.api.model.order.partner.talks.PartnerOrderForwarding;
import ru.yandex.market.tpl.api.model.order.partner.talks.PartnerOrderTalkCallerType;
import ru.yandex.market.tpl.api.model.order.partner.talks.PartnerOrderTalkInfo;
import ru.yandex.market.tpl.api.model.order.partner.talks.PartnerOrderTalksInfoDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveRequestItem;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveRequest;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeStoreRequest;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeStoreResponse;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.PartnerkaCommandEvent;
import ru.yandex.market.tpl.core.domain.partner.PartnerkaCommandRepository;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagRepository;
import ru.yandex.market.tpl.core.domain.tracker.TrackerService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.reassignment.ReassignReasonFactory;
import ru.yandex.market.tpl.core.service.company.CompanyAuthenticationService;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyRoleService;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderPhotoProvider;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderService;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderTalksService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommand;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommandService;
import ru.yandex.market.tpl.core.service.reschedule.RescheduleService;
import ru.yandex.market.tpl.core.service.reschedule.RescheduleType;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.controller.partner.PartnerOrderController;
import ru.yandex.market.tpl.internal.service.OrderCoordsUpdater;
import ru.yandex.market.tpl.internal.service.partner.PartnerOrderService;
import ru.yandex.market.tpl.internal.service.report.OrdersReportService;
import ru.yandex.market.tpl.internal.service.transferact.TransferActExportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_FIO_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

/**
 * @author a-bryukhov
 */
@WebLayerTest(PartnerOrderController.class)
class PartnerOrderControllerTest extends BaseShallowTest {

    private static final String EXTERNAL_ORDER_ID = "test123";

    @SpyBean
    private PartnerkaCommandService commandService;
    @SpyBean
    private PartnerOrderService partnerOrderService;

    @MockBean
    private PartnerkaCommandRepository commandRepository;
    @MockBean
    private PartnerReportOrderService partnerReportOrderService;
    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private OrderManager orderManager;
    @MockBean
    private UserShiftReassignManager userShiftReassignManager;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PartnerReportOrderPhotoProvider partnerReportOrderPhotoProvider;
    @MockBean
    private PartnerReportOrderTalksService partnerReportOrderTalksService;
    @MockBean
    private OrderCoordsUpdater orderCoordsUpdater;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;
    @MockBean
    private OrdersReportService ordersReportService;
    @MockBean
    private TrackerService trackerService;
    @MockBean
    private RoutingOrderTagRepository routingOrderTagRepository;
    @MockBean
    private TransferActExportService transferActExportService;
    @MockBean
    private CompanyAuthenticationService companyAuthenticationService;
    @MockBean
    private RescheduleService rescheduleService;

    @MockBean
    private ReassignReasonFactory reassignReasonFactory;
    @MockBean
    private Clock clock;

    @Test
    @DisplayName("Получение заказов по идентификатору магазина в системе Яндекс.Маркета")
    void getOrders() throws Exception {
        mockMvc.perform(get("/internal/partner/orders?page=0&size=100&senderYandexId=100922")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void shouldReturnOrders() throws Exception {
        when(partnerReportOrderService.findAll(any(), any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);

            List<PartnerReportOrderDto> orders = List.of(
                    createOrder("1566573719999", "2019-08-23", "Ильи Толстого д. 4, кв. 1"),
                    createOrder("1566573725597", "2019-08-24", "Льва Толстого д. 7, кв. 1")
            );

            return new PageImpl<>(orders, pageable, 157);
        });

        mockMvc.perform(get("/internal/partner/orders?size=2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_orders.json"), true));
    }

    @Test
    void shouldReturnEventsForOrder() throws Exception {
        PartnerOrderEventDto changeRecipientEvent =
                createOrderHistoryEventDto(3L, "2020-09-24", OrderEventType.RECIPIENT_DATA_CHANGED);
        changeRecipientEvent.setContext("Old: FIO: {full_name=1234}, email: {email=5678}, phone: {phone=90}; " +
                "New: FIO: {full_name=4321}, email: {email=8765}, phone: {phone=09}");
        PartnerOrderEventDto changeRecipientEvent2 =
                createOrderHistoryEventDto(4L, "2020-09-24", OrderEventType.RECIPIENT_DATA_CHANGED);
        changeRecipientEvent2.setContext("Old: FIO: {full_name=4321}; New: FIO: {full_name=1234}");
        when(partnerReportOrderService.findEvents(any(), any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);

            var events = List.of(
                    createOrderHistoryEventDto(1L, "2020-09-24", OrderEventType.CREATED),
                    createOrderHistoryEventDto(2L, "2020-09-24", OrderEventType.ORDER_FLOW_STATUS_CHANGED),
                    changeRecipientEvent,
                    changeRecipientEvent2
            );

            return new PageImpl<>(events, pageable, 100);
        });

        PersonalMultiTypeRetrieveRequest request = new PersonalMultiTypeRetrieveRequest().items(
                List.of(
                        new MultiTypeRetrieveRequestItem().id("1234").type(CommonTypeEnum.FULL_NAME),
                        new MultiTypeRetrieveRequestItem().id("4321").type(CommonTypeEnum.FULL_NAME),
                        new MultiTypeRetrieveRequestItem().id("5678").type(CommonTypeEnum.EMAIL),
                        new MultiTypeRetrieveRequestItem().id("8765").type(CommonTypeEnum.EMAIL),
                        new MultiTypeRetrieveRequestItem().id("90").type(CommonTypeEnum.PHONE),
                        new MultiTypeRetrieveRequestItem().id("09").type(CommonTypeEnum.PHONE))
        );

        PersonalMultiTypeRetrieveResponse response = new PersonalMultiTypeRetrieveResponse().items(
                List.of(new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.FULL_NAME).id("1234")
                                .value(new CommonType().fullName(new FullName().forename("Vasiliy").surname("Pupkin"))),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.FULL_NAME).id("4321")
                                .value(new CommonType().fullName(new FullName().forename("Andrey").surname("Andreev"))),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.EMAIL).id("5678")
                                .value(new CommonType().email("some@mail.ru")),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.EMAIL).id("8765")
                                .value(new CommonType().email("test@mail.ru")),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).id("90")
                                .value(new CommonType().phone("+79001112233")),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).id("09")
                                .value(new CommonType().phone("+79001112234"))
                )
        );

        when(personalRetrieveApi.v1MultiTypesRetrievePost(request)).thenReturn(response);
        when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.PERSONAL_DATA_STORE_AND_ENRICH_FROM_ADVICE_ENABLED)).thenReturn(true);

        mockMvc.perform(get("/internal/partner/orders/1/events?size=4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_order_events.json")));
    }

    @Test
    void shouldReturnTalksForOrder() throws Exception {
        when(partnerReportOrderTalksService.getTalksInfo("1"))
                .thenReturn(createOrderTalksInfoDto());

        mockMvc.perform(get("/internal/partner/orders/1/talks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_order_talks.json")));
    }

    @Test
    void shouldPerformUnassignOrders() throws Exception {
        Long orderId = 123L;

        when(orderRepository.findAllById(Set.of(orderId))).thenReturn(List.of());
        when(commandRepository.save(any())).thenReturn(new PartnerkaCommandEvent());

        mockMvc.perform(post("/internal/partner/orders/unassign")
                        .param("orderId", String.valueOf(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful());

        verify(orderRepository).findAllById(Set.of(orderId));
        verify(orderManager).unassignOrders(anyList(), any(), any());
    }

    @Test
    void shouldUpdateOrder() throws Exception {
        //given
        PartnerkaCommand.UpdateOrder expectedUpdateOrderCommand = PartnerkaCommand.UpdateOrder.builder()
                .deliveryDate(LocalDate.of(2020, 5, 20))
                .intervalFrom(LocalTime.of(14, 0))
                .intervalTo(LocalTime.of(18, 0))
                .recipientName("Пупкин Василий")
                .email("vasya@yandex.ru")
                .recipientPhone("+79999999999")
                .emailPersonalId(DEFAULT_EMAIL_PERSONAL_ID)
                .recipientPhonePersonalId(DEFAULT_PHONE_PERSONAL_ID)
                .recipientNamePersonalId(DEFAULT_FIO_PERSONAL_ID)
                .address(PartnerOrderAddressDto.builder()
                        .city("Москва")
                        .street("Зубовский бульвар")
                        .house("17к1с1")
                        .entrance("1")
                        .apartment("123")
                        .floor("4")
                        .build()
                )
                .externalOrderId(orderManager.getExternalOrderId(1L))
                .rescheduleDate(true)
                .build();
        given(commandRepository.save(any())).willReturn(new PartnerkaCommandEvent());

        Order order = Mockito.mock(Order.class);
        given(order.getExternalOrderId()).willReturn("123");
        given(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.PERSONAL_DATA_STORE_AND_ENRICH_FROM_ADVICE_ENABLED)).willReturn(true);
        PersonalMultiTypeStoreRequest request = new PersonalMultiTypeStoreRequest().items(
                List.of(new CommonType().email("vasya@yandex.ru"),
                        new CommonType().phone("+79999999999"),
                        new CommonType().fullName(new FullName().forename("Василий").surname("Пупкин")))
        );
        PersonalMultiTypeStoreResponse response = new PersonalMultiTypeStoreResponse().items(
                List.of(new MultiTypeStoreResponseItem().value(new CommonType().email("vasya@yandex.ru"))
                                .id(DEFAULT_EMAIL_PERSONAL_ID),
                        new MultiTypeStoreResponseItem().value(new CommonType().phone("+79999999999"))
                                .id(DEFAULT_PHONE_PERSONAL_ID),
                        new MultiTypeStoreResponseItem().value(new CommonType()
                                        .fullName(new FullName().forename("Василий").surname("Пупкин")))
                                .id(DEFAULT_FIO_PERSONAL_ID))
        );
        given(personalStoreApi.v1MultiTypesStorePost(request)).willReturn(response);
        given(orderManager.updateOrderData(expectedUpdateOrderCommand, Source.OPERATOR, Map.of())).willReturn(order);
        given(rescheduleService.getAvailableReschedulingIntervalsToValidate(any(), eq(RescheduleType.PARTNER)))
                .willReturn(Map.of());
        //when
        mockMvc.perform(patch("/internal/partner/v2/orders/1")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(getFileContent("partner/request_update_order.json"))
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful());

        //then
        verify(orderManager).updateOrderData(expectedUpdateOrderCommand, Source.OPERATOR, Map.of());
    }

    @Test
    void shouldUpdateOrderV3() throws Exception {
        //given
        String externalOrderId = "123";
        PartnerkaCommand.UpdateOrder expectedUpdateOrderCommand = PartnerkaCommand.UpdateOrder.builder()
                .deliveryDate(LocalDate.of(2020, 5, 20))
                .intervalFrom(LocalTime.of(14, 0))
                .intervalTo(LocalTime.of(18, 0))
                .recipientName("Василий Пупкин")
                .email("vasya@yandex.ru")
                .recipientPhone("+79999999999")
                .address(PartnerOrderAddressDto.builder()
                        .city("Москва")
                        .street("Зубовский бульвар")
                        .house("17к1с1")
                        .entrance("1")
                        .apartment("123")
                        .floor("4")
                        .build()
                )
                .externalOrderId(externalOrderId)
                .rescheduleDate(true)
                .build();
        given(commandRepository.save(any())).willReturn(new PartnerkaCommandEvent());

        Order order = Mockito.mock(Order.class);
        given(order.getExternalOrderId()).willReturn(externalOrderId);
        given(orderManager.updateOrderData(expectedUpdateOrderCommand, Source.CRM_OPERATOR, null)).willReturn(order);
        given(rescheduleService.getAvailableReschedulingIntervalsToValidate(any(), eq(RescheduleType.PARTNER)))
                .willReturn(null);

        //when
        mockMvc.perform(patch("/internal/partner/v3/orders/123")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(getFileContent("partner/request_update_order.json"))
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful());

        //then
        verify(orderManager).updateOrderData(expectedUpdateOrderCommand, Source.CRM_OPERATOR, null);
    }

    @Test
    @SneakyThrows
    void exportOrderTransferActs() {
        var orderExternalId = "197645";
        mockMvc.perform(
                get("/internal/partner/orders/" + orderExternalId + "/transfer-acts/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1)
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("Content-Disposition"));

        verify(transferActExportService).export(any(), eq(orderExternalId));
    }

    private PartnerReportOrderDto createOrder(String orderId, String shiftDate, String address) {
        var orderBuilder = PartnerReportOrderDto.builder();

        orderBuilder.orderId(orderId);
        orderBuilder.shiftDate(LocalDate.parse(shiftDate));
        orderBuilder.address(address);
        orderBuilder.taskStatus(OrderDeliveryTaskStatus.DELIVERY_FAILED.name());
        orderBuilder.failReasonType(OrderDeliveryTaskFailReasonType.NO_CONTACT);
        orderBuilder.courierUid(1L);
        orderBuilder.userShiftStatus(UserShiftStatus.ON_TASK);
        orderBuilder.courierName("Курьер Курьер");
        orderBuilder.sender(new OrderSenderDto("431782", "ООО Яндекс Маркет"));

        return orderBuilder.build();
    }

    private PartnerOrderEventDto createOrderHistoryEventDto(long id, String date, OrderEventType type) {
        return PartnerOrderEventDto.builder()
                .type(type)
                .source(Source.SYSTEM)
                .date(LocalDate.parse(date).atStartOfDay(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                .build();
    }

    private PartnerOrderDeliveryDto createPartnerOrderDeliveryDto() {
        PartnerRecipientDto recipient = PartnerRecipientDto.builder()
                .name("test name")
                .phone("+79998877654")
                .build();

        PartnerOrderDetailsDto orderDetails = PartnerOrderDetailsDto.builder()
                .id((long) 123)
                .orderId("test123")
                .totalPrice(BigDecimal.valueOf(20000))
                .address("Льва Толстого д. 7, кв. 1")
                .orderFlowStatus(OrderFlowStatus.CREATED)
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .orderDeliveryStatus(OrderDeliveryStatus.NOT_DELIVERED)
                .recipient(recipient)
                .cheques(List.of(new ChequeUrlDto("http://fake1.url"), new ChequeUrlDto("http://fake2.url")))
                .build();

        return PartnerOrderDeliveryDto.builder().order(orderDetails).build();
    }

    private PartnerOrderTalksInfoDto createOrderTalksInfoDto() {
        var talk = PartnerOrderTalkInfo.builder()
                .talkId("uuid")
                .callerPhone("+79991234567")
                .callerType(PartnerOrderTalkCallerType.COURIER)
                .talkTime(Instant.parse("2007-12-03T10:15:30.123Z"))
                .talkDuration(30)
                .dialTime(5)
                .succeeded(true)
                .status("status")
                .build();

        var forwarding = PartnerOrderForwarding.builder()
                .shiftDate(LocalDate.parse("2007-12-03"))
                .courierPhone("+79991234567")
                .forwardingPhone("+79998877654,12345")
                .courierName("Петров Иван Николаевич")
                .courierUid(1L)
                .courierId(2L)
                .talks(Collections.singletonList(talk))
                .build();

        return PartnerOrderTalksInfoDto.builder()
                .forwardings(Collections.singletonList(forwarding))
                .build();
    }

}
