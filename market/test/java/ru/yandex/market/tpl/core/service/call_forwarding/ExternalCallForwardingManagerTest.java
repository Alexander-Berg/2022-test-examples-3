package ru.yandex.market.tpl.core.service.call_forwarding;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.task.call.ForwardingPhoneDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.tpl.common.taxi.vgw.talks.client.api.DefaultTaxiVGWTalksApi;
import ru.yandex.market.tpl.common.taxi.vgw.talks.client.exception.TaxiVoiceGatewayTalksTalksNotFoundException;
import ru.yandex.market.tpl.common.taxi.vgw.talks.client.model.NotFound;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.api.DefaultTaxiVGWForwardingsApi;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.exception.TaxiVoiceGatewayBadGatewayException;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.exception.TaxiVoiceGatewayBadRequestException;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadGateway;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadGatewayErrorCode;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadRequest;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadRequestErrorCode;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.ForwardingItem;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.PostForwardingsResponse;
import ru.yandex.market.tpl.common.util.exception.TplExternalException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestPoint;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.ExternalCallForwarding;
import ru.yandex.market.tpl.core.domain.usershift.ExternalCallForwardingRepository;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CALL_FORWARDING_FOR_CLIENT_RETURN_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.PERSONAL_DATA_TELEPHONY_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.TAXI_VGW_TELEPHONY_NEW_TTL_SECONDS;

@RequiredArgsConstructor
public class ExternalCallForwardingManagerTest extends TplAbstractTest {
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final Clock clock;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final CallToRecipientTaskRepository callToRecipientTaskRepository;
    private final ExternalCallForwardingManager callForwardingManager;
    private final ExternalCallForwardingQueryService callForwardingQueryService;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final DefaultTaxiVGWForwardingsApi taxiVoiceGatewayForwardingApi;
    private final DefaultTaxiVGWTalksApi taxiVGWTalksApi;
    private final DefaultPersonalRetrieveApi personalRetrieveApi;
    private final ExternalCallForwardingRepository externalCallForwardingRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TestDataFactory testDataFactory;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final ExternalCallForwardingMapper externalCallForwardingMapper;

    private User user;
    private Shift shift;
    private CallToRecipientTask callToRecipientTask;
    private Order order;
    private UserShift userShift;
    private static final String RECIPIENT_PHONE = "+79998765432";
    private static final String RECIPIENT_PERSONAL_PHONE_ID = "abc1234";
    private static final String RECIPIENT_ADDRESS = "ул. Ленина, д. 3, подъезд 1, кв. 1, этаж 1, домофон 1";
    private static final Integer ONE_DAY_SECONDS = 60 * 60 * 24;

    @AfterEach
    void clear() {
        Mockito.reset(taxiVoiceGatewayForwardingApi);
        Mockito.reset(taxiVGWTalksApi);
    }

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        transactionTemplate.executeWithoutResult(ts ->
                userPropertyService.addPropertyToUser(
                        user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true
                )
        );
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        LocalDate now = LocalDate.now(clock);
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(now)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .street("ул. Ленина")
                                .house("3")
                                .entrance("1")
                                .entryPhone("1")
                                .floor(1)
                                .apartment("1")
                                .build())
                        .recipientPhone(RECIPIENT_PHONE)
                        .recipientPersonalPhoneId(RECIPIENT_PERSONAL_PHONE_ID)
                        .build());
        var task = helper.taskUnpaid(RECIPIENT_ADDRESS, 12, order.getId());
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(task)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        callToRecipientTask = transactionTemplate.execute(action -> {
            userShift = userShiftRepository.findByIdWithRoutePoints(userShiftId).orElseThrow();
            return userShift.streamRoutePoints().flatMap(RoutePoint::streamOrderDeliveryTasks)
                    .map(OrderDeliveryTask::getCallToRecipientTask)
                    .findFirst().orElseThrow();
        });
    }

    @Test
    void checkForwardingCreatedWhenVgwApiError() {
        activateFeatureFlag();

        when(taxiVoiceGatewayForwardingApi.v1ForwardingsPost(any())).thenThrow(
                new TaxiVoiceGatewayBadRequestException(new BadRequest()
                        .code(BadRequestErrorCode.REGIONISNOTSUPPORTED)
                )
        );
        TaxiVoiceGatewayBadRequestException exception = assertThrows(
                TaxiVoiceGatewayBadRequestException.class,
                () -> callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId())
        );
        assertThat(exception).isNotNull();
        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertThat(callForwarding).extracting(ExternalCallForwarding::getForwardingPhone).isNull();
    }

    @Test
    void checkCallToRecipientTaskHasPhoneWhenVgwApiSuccess() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79995551122", "1234", now);

        transactionTemplate.execute(status -> {
            var callTask = callToRecipientTaskRepository.findById(callToRecipientTask.getId());
            assertThat(callTask).isPresent();
            assertThat(callTask.get().getForwarding()).isEqualTo(callForwarding);
            return null;
        });
    }

    @Test
    void mapForwardingPhoneDtoWithActivateFeatureFlag() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        ForwardingPhoneDto dto = externalCallForwardingMapper.map(callToRecipientTask.getId());
        assertThat(dto.getForwardingPhone()).isEqualTo("+79995551122,1234");
    }

    @Test
    void mapForwardingPhoneDtoWhenFlagWasActiveButAfterBecameNotActive() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        disableFeatureFlag();
        userHelper.clearUserPropertiesCache();
        ForwardingPhoneDto dto = externalCallForwardingMapper.map(callToRecipientTask.getId());
        assertThat(dto.getForwardingPhone()).isEqualTo(null);
    }

    @Test
    void mapForwardingPhoneDtoWhenDoesntCreateForwardingButFlagWasActive() {
        activateFeatureFlag();

        ForwardingPhoneDto dto = externalCallForwardingMapper.map(callToRecipientTask.getId());
        assertThat(dto.getForwardingPhone()).isEqualTo(null);
    }

    @Test
    void checkCallToRecipientTaskHasPhoneWhenVgwApiSuccessWithClientReturn() {
        activateFeatureFlag();
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CALL_FORWARDING_FOR_CLIENT_RETURN_ENABLED)).thenReturn(true);
        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation("+79995551122", "1234", now);

        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        clientReturn.getClient().getClientData().setPhone(RECIPIENT_PHONE);
        clientReturn.setLogisticRequestPointFrom(
                LogisticRequestPoint.builder()
                        .originalLatitude(BigDecimal.ONE)
                        .originalLongitude(BigDecimal.TEN)
                        .street("ул. Ленина")
                        .house("3")
                        .entrance("1")
                        .entryPhone("1")
                        .floor("1")
                        .apartment("1")
                        .buildWithAdress()
        );
        clientReturnRepository.save(clientReturn);

        commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        var tod = orderDeliveryTaskRepository.findAllByClientReturnId(clientReturn.getId()).get(0);

        callForwardingManager.tryToUpdateForwarding(tod.getCallToRecipientTask().getId());

        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79995551122", "1234", now);

        transactionTemplate.execute(status -> {
            var callTask = callToRecipientTaskRepository.findById(tod.getCallToRecipientTask().getId());
            assertThat(callTask).isPresent();
            assertThat(callTask.get().getForwarding()).isEqualTo(callForwarding);
            return null;
        });
    }

    @Test
    void checkChangeVgwApiRequestWhenIdempotencyConflict() {
        activateFeatureFlag();
        configurationServiceAdapter.insertValue(TAXI_VGW_TELEPHONY_NEW_TTL_SECONDS, ONE_DAY_SECONDS);

        OffsetDateTime now = OffsetDateTime.now(clock);
        when(taxiVoiceGatewayForwardingApi.v1ForwardingsPost(any())).thenThrow(
                new TaxiVoiceGatewayBadRequestException(new BadRequest()
                        .code(BadRequestErrorCode.IDEMPOTENCYCONFLICT)
                )
        );

        when(taxiVoiceGatewayForwardingApi.v1ForwardingsGet(any(), any(), any(), any(), any())).thenReturn(
                List.of(new ForwardingItem()
                        .phone("+79995551122")
                        .ext("1234")
                        .createdAt(now)
                )
        );
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertForwarding(
                callForwarding, "+79995551122", "1234",
                now.plusSeconds(ONE_DAY_SECONDS)
        );

        transactionTemplate.execute(status -> {
            var callTask = callToRecipientTaskRepository.findById(callToRecipientTask.getId());
            assertThat(callTask).isPresent();
            assertThat(callTask.get().getForwarding()).isEqualTo(callForwarding);
            return null;
        });
    }

    @Test
    void checkHideClientPhoneFlagShouldNotCreated() {
        transactionTemplate.execute(action -> {
            userPropertyService.addPropertyToUser(user, UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER,
                    false);
            return null;
        });
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isFalse();
    }

    @Test
    void checkHideClientPhoneRatioShouldCreatedAlways() {
        activateFeatureFlag();

        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isTrue();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isTrue();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isTrue();

    }

    @Test
    void checkHideClientPhoneRatioShouldNotCreated() {
        transactionTemplate.execute(action -> {
            userPropertyService.addPropertyToUser(user, UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER,
                    true);
            userPropertyService.addPropertyToUser(user, UserProperties.TASKS_WITH_PHONE_FORWARDING_RATIO,
                    BigDecimal.ZERO);
            return null;
        });

        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isFalse();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isFalse();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isFalse();
    }

    @Test
    void checkHideClientPhoneRatioDeterministicOnCallTaskId() {
        transactionTemplate.execute(action -> {
            userPropertyService.addPropertyToUser(user, UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER,
                    true);
            userPropertyService.addPropertyToUser(user, UserProperties.TASKS_WITH_PHONE_FORWARDING_RATIO,
                    new BigDecimal("0.2"));
            return null;
        });

        var callTaskMock = Mockito.mock(CallToRecipientTask.class);
        doReturn(userShift).when(callTaskMock).getUserShift();

        doReturn(1L).when(callTaskMock).getId();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callTaskMock)).isTrue();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callTaskMock)).isTrue();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callTaskMock)).isTrue();

        doReturn(2L).when(callTaskMock).getId();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callTaskMock)).isFalse();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callTaskMock)).isFalse();
        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callTaskMock)).isFalse();
    }

    @Test
    void whenNotExistsCallToRecipientTask_thenNotThrowException() {
        assertDoesNotThrow(() -> callForwardingManager.tryToUpdateForwarding(-100500L));
    }

    @Test
    void savedInfoAboutForwardingToHistory() {
        activateFeatureFlag();

        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        List<OrderHistoryEvent> orderHistoryEvents = orderHistoryEventRepository.findAllByOrderId(order.getId())
                .stream()
                .filter(orderHistoryEvent -> orderHistoryEvent.getType() == OrderEventType.CALL_FORWARDING_UPDATED)
                .collect(Collectors.toList());

        assertThat(orderHistoryEvents.size()).isEqualTo(1);
    }

    @Test
    void whenActivateFlagsByScWithoutActiveUserShift_ThenFlagsActivate() {
        transactionTemplate.execute(ts -> {
            sortingCenterPropertyService.upsertPropertyToSortingCenter(
                    shift.getSortingCenter(), UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER, true
            );
            sortingCenterPropertyService.upsertPropertyToSortingCenter(
                    shift.getSortingCenter(), UserProperties.TASKS_WITH_PHONE_FORWARDING_RATIO, BigDecimal.ONE
            );
            return null;
        });

        assertThat(callForwardingManager.isNeedToHideClientPhoneNumber(callToRecipientTask)).isTrue();
    }

    @Test
    void throwException_WhenTaxiVGWForwardingsApiReturnNull() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation(null, null, now);
        assertThrows(
                TplIllegalStateException.class,
                () -> callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId())
        );

    }

    @Test
    void checkForwardingDoNotUpdatedWhenVgwApiReturnSameForwarding() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);

        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79995551122", "1234", now);

        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79995551122", "1234", now);
    }

    @Test
    void checkForwardingDoNotUpdatedWhenVgwApiReturnNewForwarding() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);

        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79995551122", "1234", now);

        mockForwardingCreation("+79001230000", "3322", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79001230000", "3322", now);
    }

    @Test
    void checkForwardingCreatedWhenCallTryToUpdateBeforeCreationForwarding() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);

        mockForwardingCreation("+79001230000", "3322", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79001230000", "3322", now);

        transactionTemplate.execute(status -> {
            var callTask = callToRecipientTaskRepository.findById(callToRecipientTask.getId());
            assertThat(callTask).isPresent();
            assertThat(callTask.get().getForwarding()).isEqualTo(callForwarding);
            return null;
        });
    }

    @Test
    void checkCallTaskHasReferenceToForwardingWhenUpdateForwarding() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);

        mockForwardingCreation("+79001230000", "3322", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        ExternalCallForwarding callForwarding = findForwardingInDb();
        assertForwarding(callForwarding, "+79001230000", "3322", now);

        transactionTemplate.execute(status -> {
            var callTask = callToRecipientTaskRepository.findById(callToRecipientTask.getId());
            assertThat(callTask).isPresent();
            assertThat(callTask.get().getForwarding()).isEqualTo(callForwarding);
            return null;
        });
    }

    @Test
    void checkSavedInfoAboutForwardingToHistoryWhenUpdateForwarding() {
        activateFeatureFlag();

        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        mockForwardingCreation("+79001230000", "3322", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());


        List<OrderHistoryEvent> orderHistoryEvents = orderHistoryEventRepository.findAllByOrderId(order.getId())
                .stream()
                .filter(orderHistoryEvent -> orderHistoryEvent.getType() == OrderEventType.CALL_FORWARDING_UPDATED)
                .collect(Collectors.toList());

        assertThat(orderHistoryEvents.size()).isEqualTo(2);
    }

    @Test
    void checkNotSavedInfoAboutForwardingToHistoryWhenDoNotUpdateForwarding() {
        activateFeatureFlag();

        OffsetDateTime now = OffsetDateTime.now(clock);
        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());


        List<OrderHistoryEvent> orderHistoryEvents = orderHistoryEventRepository.findAllByOrderId(order.getId())
                .stream()
                .filter(orderHistoryEvent -> orderHistoryEvent.getType() == OrderEventType.CALL_FORWARDING_UPDATED)
                .collect(Collectors.toList());

        assertThat(orderHistoryEvents.size()).isEqualTo(1);
    }

    @Test
    void getForwardingsForCallTask_WhenVgwApiSuccess() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);

        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        var forwarding = findForwardingInDb();

        var forwardingItem = new ForwardingItem()
                .phone(forwarding.getForwardingPhone())
                .ext(forwarding.getForwardingExt())
                .createdAt(now);
        when(taxiVoiceGatewayForwardingApi.v1ForwardingsGet(any(), any(), any(), any(), any()))
                .thenReturn(List.of(forwardingItem));

        var callTask = callToRecipientTaskRepository
                .findByIdWithShiftAndTasksAndUser(callToRecipientTask.getId())
                .orElseThrow();
        var actualForwardingItems = callForwardingManager.getForwardingsForCallTask(
                callTask,
                externalCallForwardingRepository.findByIdOrThrow(callTask.getForwarding().getId())
        );

        assertThat(actualForwardingItems).asList().containsOnly(forwardingItem);
        var actualForwardingItem = actualForwardingItems.get(0);
        assertThat(actualForwardingItem.getPhone()).isEqualTo(forwarding.getForwardingPhone());
        assertThat(actualForwardingItem.getExt()).isEqualTo(forwarding.getForwardingExt());
    }

    @Test
    void getForwardingsForCallTask_WhenVgwApiBadCall() {
        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);

        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());

        when(taxiVoiceGatewayForwardingApi.v1ForwardingsGet(any(), any(), any(), any(), any()))
                .thenThrow(new TaxiVoiceGatewayBadGatewayException(new BadGateway()
                        .code(BadGatewayErrorCode.PARTNERINTERNALERROR)
                        .message("message")
                ));

        var callTask = callToRecipientTaskRepository
                .findByIdWithShiftAndTasksAndUser(callToRecipientTask.getId())
                .orElseThrow();
        assertThrows(
                TplExternalException.class,
                () -> callForwardingManager.getForwardingsForCallTask(
                        callTask, externalCallForwardingRepository.findByIdOrThrow(callTask.getForwarding().getId())
                )
        );
    }

    @Test
    void getTalkRecording_WhenVgwApiError() {
        activateFeatureFlag();
        when(taxiVGWTalksApi.v1TalkGetWithHttpInfo(any(), any()))
                .thenThrow(new TaxiVoiceGatewayTalksTalksNotFoundException(new NotFound()))
        ;

        assertThrows(TplEntityNotFoundException.class, () -> callForwardingManager.getTalkRecording("something", null));
    }

    @Test
    void getTalkRecording_WhenPassEmptyTalkId() {
        assertAll(() -> {
            assertThrows(TplIllegalArgumentException.class,
                    () -> callForwardingManager.getTalkRecording(null, null));
            assertThrows(TplIllegalArgumentException.class,
                    () -> callForwardingManager.getTalkRecording("", null));
            assertThrows(TplIllegalArgumentException.class,
                    () -> callForwardingManager.getTalkRecording("  ", null));
        });
    }

    @Test
    void getPhoneNumber_WhenPersonalData() {
        configurationServiceAdapter.mergeValue(PERSONAL_DATA_TELEPHONY_ENABLED, true);
        MultiTypeRetrieveResponseItem responseItem =
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).value(new CommonType().phone(RECIPIENT_PHONE)).id(RECIPIENT_PERSONAL_PHONE_ID);
        PersonalMultiTypeRetrieveResponse value = new PersonalMultiTypeRetrieveResponse()
                .items(List.of(responseItem));
        when(personalRetrieveApi.v1MultiTypesRetrievePost(any())).thenReturn(value);

        activateFeatureFlag();
        OffsetDateTime now = OffsetDateTime.now(clock);

        mockForwardingCreation("+79995551122", "1234", now);
        callForwardingManager.tryToUpdateForwarding(callToRecipientTask.getId());
        var forwarding = findForwardingInDb();

        var forwardingItem = new ForwardingItem()
                .phone(forwarding.getForwardingPhone())
                .ext(forwarding.getForwardingExt())
                .createdAt(now);
        when(taxiVoiceGatewayForwardingApi.v1ForwardingsGet(any(), any(), any(), any(), any()))
                .thenReturn(List.of(forwardingItem));

        var callTask = callToRecipientTaskRepository
                .findByIdWithShiftAndTasksAndUser(callToRecipientTask.getId())
                .orElseThrow();
        var actualForwardingItems = callForwardingManager.getForwardingsForCallTask(
                callTask,
                externalCallForwardingRepository.findByIdOrThrow(callTask.getForwarding().getId())
        );

        assertThat(actualForwardingItems).asList().containsOnly(forwardingItem);
        var actualForwardingItem = actualForwardingItems.get(0);
        assertThat(actualForwardingItem.getPhone()).isEqualTo(forwarding.getForwardingPhone());
        assertThat(actualForwardingItem.getExt()).isEqualTo(forwarding.getForwardingExt());
    }

    private void assertForwarding(ExternalCallForwarding forwarding, String phone, String ext,
                                  OffsetDateTime expiresAt) {
        assertThat(forwarding.getForwardingPhone()).isEqualTo(phone);
        assertThat(forwarding.getForwardingExt()).isEqualTo(ext);
        assertThat(forwarding.getForwardingExpiresAt()).isEqualTo(expiresAt.toInstant());
    }


    private ExternalCallForwarding findForwardingInDb() {
        Optional<ExternalCallForwarding> callForwardingOptional =
                callForwardingQueryService.findByExternalRef(
                        callForwardingManager.constructRefId(shift.getId(), user.getPhone(), RECIPIENT_PHONE,
                                RECIPIENT_ADDRESS)
                );
        var temp = externalCallForwardingRepository.findAll();
        assertThat(callForwardingOptional).isPresent();
        return callForwardingOptional.get();
    }

    private void mockForwardingCreation(String phone, String ext, OffsetDateTime now) {
        when(taxiVoiceGatewayForwardingApi.v1ForwardingsPost(any())).thenReturn(
                new PostForwardingsResponse()
                        .phone(phone)
                        .ext(ext)
                        .expiresAt(now)
        );
    }

    private void activateFeatureFlag() {
        transactionTemplate.execute(action -> {
            userPropertyService.addPropertyToUser(user, UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER,
                    true);
            userPropertyService.addPropertyToUser(user, UserProperties.TASKS_WITH_PHONE_FORWARDING_RATIO,
                    BigDecimal.ONE);
            return null;
        });
    }

    private void disableFeatureFlag() {
        transactionTemplate.execute(action -> {
            userPropertyService.addPropertyToUser(user, UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER, false);
            userPropertyService.addPropertyToUser(user, UserProperties.TASKS_WITH_PHONE_FORWARDING_RATIO,
                    BigDecimal.ONE);
            return null;
        });
    }

    private ExternalCallForwarding createForwarding() {
        return ExternalCallForwarding.builder()
                .externalRef(callForwardingManager.constructRefId(
                        shift.getId(),
                        user.getPhone(),
                        order.getDelivery().getRecipientPhone(),
                        order.getDelivery().getDeliveryAddress().getAddress())
                )
                .uuid(UUID.randomUUID().toString())
                .forwardingPhone("forwardingPhone")
                .forwardingExt("forwardingExt")
                .build();
    }
}
