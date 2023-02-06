package ru.yandex.market.tpl.core.service.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.partner.talks.PartnerOrderForwarding;
import ru.yandex.market.tpl.api.model.order.partner.talks.PartnerOrderTalkCallerType;
import ru.yandex.market.tpl.api.model.order.partner.talks.PartnerOrderTalkInfo;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.api.DefaultTaxiVGWForwardingsApi;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.exception.TaxiVoiceGatewayBadGatewayException;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadGateway;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadGatewayErrorCode;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.CallResult;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.ForwardingItem;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.PostForwardingsResponse;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.TalkItem;
import ru.yandex.market.tpl.common.util.exception.TplExternalException;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.call_forwarding.ExternalCallForwardingManager;
import ru.yandex.market.tpl.core.service.call_forwarding.ForwardingRecipientPhoneFormatter;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class PartnerReportOrderTalksServiceTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final Clock clock;

    private final PartnerReportOrderTalksService service;

    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final ExternalCallForwardingManager callForwardingManager;
    private final ForwardingRecipientPhoneFormatter forwardingRecipientPhoneFormatter;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final DefaultTaxiVGWForwardingsApi taxiVoiceGatewayForwardingApi;

    private User user;
    private Shift shift;
    private CallToRecipientTask callToRecipientTask;
    private Order order;

    private static final String RECIPIENT_PHONE = "+79998765432";
    private static final String RECIPIENT_ADDRESS = "ул. Ленина, д. 3, подъезд 1, кв. 1, этаж 1, домофон 1";
    private static final String FORWARDING_PHONE = "+79995551122";
    private static final String FORWARDING_EXT = "12345";
    private static final String CALL_STATUS = "status";
    private static final int CALL_DURATION = 123;
    private static final String TALK_UUID = "uuid";

    @AfterEach
    void clear() {
        Mockito.reset(taxiVoiceGatewayForwardingApi);
    }

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        LocalDate now = LocalDate.now(clock);

        user = userHelper.findOrCreateUser(824125L, now);
        shift = userHelper.findOrCreateOpenShift(now);
        order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(now)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .street("ул. Ленина")
                                .house("3")
                                .entrance("1")
                                .entryPhone("1")
                                .floor(1)
                                .apartment("1")
                                .build())
                        .recipientPhone(RECIPIENT_PHONE)
                        .build()
        );
        callToRecipientTask = createDeliveryAndCallTask(order);
    }

    @Test
    void getTalksInfo_WhenNoForwardings() {
        var talksInfoDto = service.getTalksInfo(order.getExternalOrderId()).getForwardings();
        assertForwardingDto(talksInfoDto, 0);
        talksInfoDto.forEach(forwarding -> assertTalkDto(forwarding.getTalks(), 0, Instant.now(clock)));
    }

    @Test
    void getTalksInfo_WhenForwardingCreatedWithNoTalks() {
        activateFeatureFlag();
        var now = OffsetDateTime.now(clock);
        mockForwardingCreation(FORWARDING_PHONE, FORWARDING_EXT, now);

        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        mockForwardingsGet(List.of(new ForwardingItem()
                .requesterPhone(user.getPhone())
                .phone(FORWARDING_PHONE)
                .ext(FORWARDING_EXT)));

        var forwardings = service.getTalksInfo(order.getExternalOrderId()).getForwardings();
        assertForwardingDto(forwardings, 1);
        forwardings.forEach(forwarding -> assertTalkDto(forwarding.getTalks(), 0, Instant.now(clock)));
    }

    @Test
    void getTalksInfo_WhenForwardingCreated() {
        activateFeatureFlag();
        var now = OffsetDateTime.now(clock);
        mockForwardingCreation(FORWARDING_PHONE, FORWARDING_EXT, now);

        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        mockForwardingsGet(List.of(createForwardingItem(now)));

        var forwardings = service.getTalksInfo(order.getExternalOrderId()).getForwardings();
        assertForwardingDto(forwardings, 1);
        forwardings.forEach(forwarding -> assertTalkDto(forwarding.getTalks(), 1, Instant.now(clock)));
    }

    @Test
    void getTalksInfo_WhenSeveralForwardingCreatedAndSeveralTasks() {
        activateFeatureFlag();
        var now = OffsetDateTime.now(clock);
        var secondCallTask = createDeliveryAndCallTask(order);

        mockForwardingCreation(FORWARDING_PHONE, FORWARDING_EXT, now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        callForwardingManager.tryToUpdateForwarding(secondCallTask.getId());

        mockForwardingsGet(List.of(createForwardingItem(now), createForwardingItem(now)));

        var forwardings = service.getTalksInfo(order.getExternalOrderId()).getForwardings();
        assertForwardingDto(forwardings, 4);
        forwardings.forEach(forwarding -> assertTalkDto(forwarding.getTalks(), 1, Instant.now(clock)));
    }

    @Test
    void getTalksInfo_WhenExternalCallFail() {
        activateFeatureFlag();
        mockForwardingCreation(FORWARDING_PHONE, FORWARDING_EXT, OffsetDateTime.now(clock));
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        when(taxiVoiceGatewayForwardingApi.v1ForwardingsGet(any(), any(), any(), any(), any()))
                .thenThrow(createTaxiException());

        assertThrows(TplExternalException.class, () -> service.getTalksInfo(order.getExternalOrderId()));
    }

    @Test
    void getTalksInfo_WhenForwardingWithAnotherOffset_ThenAtLocalTime() {
        activateFeatureFlag();
        var now = OffsetDateTime.now(clock);
        mockForwardingCreation(FORWARDING_PHONE, FORWARDING_EXT, now);

        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        mockForwardingsGet(List.of(createForwardingItem(now.withOffsetSameInstant(ZoneOffset.ofHours(5)))));

        var forwardings = service.getTalksInfo(order.getExternalOrderId()).getForwardings();
        assertForwardingDto(forwardings, 1);
        forwardings.forEach(forwarding -> assertTalkDto(forwarding.getTalks(), 1, Instant.now(clock)));
    }

    @Test
    void getTalksInfo_WhenExternalOptionalsAreMissing() {
        activateFeatureFlag();
        var now = OffsetDateTime.now(clock);
        mockForwardingCreation(FORWARDING_PHONE, FORWARDING_EXT, now);

        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        var talkItem = new TalkItem()
                .id(TALK_UUID)
                .startedAt(now)
                .length(CALL_DURATION);
        var forwardingItem = new ForwardingItem()
                .requesterPhone(user.getPhone())
                .talks(List.of(talkItem));
        mockForwardingsGet(List.of(forwardingItem));

        var formattedPhone = forwardingRecipientPhoneFormatter.format(null, null);
        var forwardings = service.getTalksInfo(order.getExternalOrderId()).getForwardings();
        assertThat(forwardings).asList()
                .hasSize(1)
                .extracting("shiftDate", "courierUid", "courierId", "courierName",
                        "courierPhone", "forwardingPhone")
                .containsExactly(Tuple.tuple(shift.getShiftDate(), user.getUid(), user.getId(), user.getName(),
                        user.getPhone(), formattedPhone));
        assertThat(forwardings.get(0).getTalks()).asList()
                .hasSize(1)
                .extracting("talkId", "talkTime", "status", "dialTime", "callerPhone",
                        "callerType", "talkDuration", "succeeded")
                .containsExactly(Tuple.tuple(TALK_UUID, Instant.now(clock), null, null, null,
                        PartnerOrderTalkCallerType.UNKNOWN, CALL_DURATION, null));
    }

    private CallToRecipientTask createDeliveryAndCallTask(Order order) {
        var task = helper.taskUnpaid(RECIPIENT_ADDRESS, 12, order.getId());
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(task)
                .build();

        long userShiftId = userShiftCommandService.createUserShift(createCommand);
        return transactionTemplate.execute(action -> {
            UserShift userShift = userShiftRepository.findByIdWithRoutePoints(userShiftId).orElseThrow();
            return userShift.streamRoutePoints().flatMap(RoutePoint::streamOrderDeliveryTasks)
                    .map(OrderDeliveryTask::getCallToRecipientTask)
                    .findFirst().orElseThrow();
        });
    }

    private void activateFeatureFlag() {
        transactionTemplate.execute(action -> {
            userPropertyService.addPropertyToUser(user, UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER, true);
            userPropertyService.addPropertyToUser(user, UserProperties.TASKS_WITH_PHONE_FORWARDING_RATIO,
                    BigDecimal.ONE);
            return null;
        });
    }

    private void mockForwardingCreation(String phone, String ext, OffsetDateTime now) {
        when(taxiVoiceGatewayForwardingApi.v1ForwardingsPost(any())).thenReturn(
                new PostForwardingsResponse()
                        .phone(phone)
                        .ext(ext)
                        .expiresAt(now)
        );
    }

    private void mockForwardingsGet(List<ForwardingItem> forwardingItems) {
        when(taxiVoiceGatewayForwardingApi.v1ForwardingsGet(any(), any(), any(), any(), any()))
                .thenReturn(forwardingItems);
    }

    private Exception createTaxiException() {
        return new TaxiVoiceGatewayBadGatewayException(new BadGateway()
                .code(BadGatewayErrorCode.PARTNERINTERNALERROR)
                .message("message")
        );
    }

    private ForwardingItem createForwardingItem(OffsetDateTime startedAt) {
        var talkItem = new TalkItem()
                .id(TALK_UUID)
                .startedAt(startedAt)
                .callResult(new CallResult().status(CALL_STATUS).succeeded(true))
                .length(CALL_DURATION)
                .callerPhone(user.getPhone());
        return new ForwardingItem()
                .phone(FORWARDING_PHONE)
                .ext(FORWARDING_EXT)
                .requesterPhone(user.getPhone())
                .talks(List.of(talkItem));
    }

    private void assertForwardingDto(List<PartnerOrderForwarding> forwardings, int expectedSize) {
        if (expectedSize == 0) {
            assertThat(forwardings).asList().isEmpty();
        } else {
            var formattedPhone = forwardingRecipientPhoneFormatter.format(FORWARDING_PHONE, FORWARDING_EXT);
            assertThat(forwardings).asList().hasSize(expectedSize);
            forwardings.forEach(forwarding -> assertThat(forwarding)
                    .extracting("shiftDate", "courierUid", "courierId", "courierName", "courierPhone",
                            "forwardingPhone")
                    .containsExactly(shift.getShiftDate(), user.getUid(), user.getId(), user.getName(), user.getPhone(),
                            formattedPhone));
        }
    }

    private void assertTalkDto(List<PartnerOrderTalkInfo> talks, int expectedSize, Instant talkTime) {
        if (expectedSize == 0) {
            assertThat(talks).asList().isEmpty();
        } else {
            assertThat(talks).asList().hasSize(expectedSize);
            talks.forEach(talk -> assertThat(talk)
                    .extracting("talkId", "talkTime", "status", "dialTime", "callerPhone",
                            "callerType", "talkDuration", "succeeded")
                    .containsExactly(TALK_UUID, talkTime, CALL_STATUS, null, user.getPhone(),
                            PartnerOrderTalkCallerType.COURIER, CALL_DURATION, true));
        }
    }
}
