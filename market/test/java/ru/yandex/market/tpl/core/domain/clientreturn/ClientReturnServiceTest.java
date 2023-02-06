package ru.yandex.market.tpl.core.domain.clientreturn;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.manual.CreateClientReturnRoutePointRequestDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.precise.PreciseGeoPointService;
import ru.yandex.market.tpl.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.CLOSED;
import static ru.yandex.market.tpl.core.dbqueue.model.QueueType.CLIENT_RETURN_VERIFY_COORDS;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CLIENT_RETURN_SCAN_ON_SC_VALIDATION_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CLIENT_RETURN_TASK_MAPPING_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.FINISH_CLIENT_RETURN_TASK_IDEMPOTENCY_CHECK_DISABLED;


@RequiredArgsConstructor
class ClientReturnServiceTest extends TplAbstractTest {

    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TransactionTemplate transactionTemplate;
    private final ClientReturnService clientReturnService;
    private final UserShiftCommandService commandService;
    private final SortingCenterService sortingCenterService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TestUserHelper testUserHelper;
    private final ReturnsApi returnsApi;
    private final Clock clock;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper helper;
    private final SortingCenterRepository sortingCenterRepository;
    private final JmsTemplate jmsTemplate;
    private final UserShiftTestHelper userShiftTestHelper;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final UserPropertyService userPropertyService;
    private final TestDataFactory testDataFactory;

    private final SqsQueueProperties sqsQueueProperties = Mockito.mock(SqsQueueProperties.class);

    private static final List<String> barcodes = List.of(
            ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX + "123456"
    );
    private static final String QUEUE = "client_return";
    private static final String SOURCE = "courrier";
    private static final String INCORRECT_PREFIX = "FSN_RET_";

    private OrderDeliveryTask clientReturnTask;
    private ClientReturn clientReturn;
    private User user;
    private UserShift userShift;

    @MockBean
    private final PreciseGeoPointService preciseGeoPointService;

    @BeforeEach
    void init() {
        //ставим отличный от null токен, чтобы не притащить в список сц лишние дс
        transactionTemplate.executeWithoutResult(
                cmd -> sortingCenterRepository.findAll()
                        .stream()
                        .flatMap(it -> sortingCenterService.findDsForSortingCenter(it.getId()).stream())
                        .forEach(it -> it.setToken("1"))
        );

        user = testUserHelper.findOrCreateUser(1L);
        transactionTemplate.executeWithoutResult(ts -> userPropertyService.addPropertyToUser(user,
                UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true));
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        clientReturn = clientReturnGenerator.generateReturnFromClient();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 13, clientReturn.getId()))
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);

        userShift = transactionTemplate.execute(
                cmd -> {
                    var temp = userShiftRepository.findByIdOrThrow(userShiftId);
                    Hibernate.initialize(temp.streamRoutePoints().collect(Collectors.toList()));
                    return temp;
                }
        );

        assert userShift != null;

        clientReturnTask =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getClientReturnId(), clientReturn.getId()))
                        .findFirst()
                        .orElseThrow();

        //region Приходим на пикаппоинт в начале дня

        testUserHelper.finishPickupAtStartOfTheDay(userShiftId, true);
        //endregion

        var clientReturnTask =
                userShift.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst()
                        .orElseThrow();
        var clientReturnRoutePoint = clientReturnTask.getRoutePoint();

        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), clientReturnRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId())
        ));

        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CLIENT_RETURN_TASK_MAPPING_ENABLED))
                .thenReturn(true);
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );
        Mockito.when(returnsApi.commitReturnWithHttpInfo(anyLong())).thenReturn(
                ResponseEntity.ok().build()
        );
        Mockito.when(sqsQueueProperties.getOutQueue())
                .thenReturn(QUEUE);
        Mockito.when(sqsQueueProperties.getSource())
                .thenReturn(SOURCE);
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                        ConfigurationProperties.CREATING_CLIENT_RETURN_IN_SC_DISABLED
                ))
                .thenReturn(false);
    }

    @Test
    @DisplayName("Проверка работы прикрепления к клиентскому возврату штрих кода и завершения таски на доставку.")
    void clientReturnBarcodeAttached() {
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );

        // баркод пустой
        assertThat(clientReturn.getBarcode()).isNull();
        //до завершения таски таска в состоянии NOT_DELIVERED
        var preTestClientReturnTask = orderDeliveryTaskRepository.findById(clientReturnTask.getId()).orElseThrow();
        Assertions.assertThat(preTestClientReturnTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        var firstItemId = clientReturn.getItems().get(0).getId();
        clientReturnService.assignBarcodeAndFinishTask(
                barcodes,
                Map.of(firstItemId, "comment"),
                clientReturn.getExternalReturnId(),
                user,
                clientReturnTask.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

        //баркод у объекта поменялся
        assertThat(clientReturn.getBarcode()).isEqualTo(barcodes.get(0));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_ATTACH_BARCODE, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CLIENT_RETURN_ATTACH_BARCODE);

        clientReturn = clientReturnRepository.findByExternalReturnIdWithItems(clientReturn.getExternalReturnId())
                .orElseThrow();

        //Сам возврат изменил свой статус
        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.RECEIVED);
        //Заполнен коммент
        var firstItem = StreamEx.of(clientReturn.getItems())
                .findFirst(i -> i.getId().equals(firstItemId))
                .orElseThrow();
        assertThat(firstItem.getCourierComment()).isEqualTo("comment");

        //появился ивент на отправку в лес
        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_CLIENT_RETURN, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_CLIENT_RETURN);

        //отправка в лес отработала
        Mockito.verify(jmsTemplate, Mockito.atLeastOnce()).convertAndSend(anyString(), any(Event.class));
        //после завершения таска закрылась
        var postTestClientReturnTask = orderDeliveryTaskRepository.findById(clientReturnTask.getId()).orElseThrow();
        assertThat(postTestClientReturnTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
    }

    @DisplayName("Проверка, что при ошибочном ответе от ReturnsApi отправка в лес не пройдет.")
    @Test
    void failedToSendToLes_WhenReturnsApiCannotProcessUpdate() {
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.badRequest().build()
        );

        // баркод пустой
        assertThat(clientReturn.getBarcode()).isNull();
        //до завершения таски таска в состоянии NOT_DELIVERED
        var preTestClientReturnTask = orderDeliveryTaskRepository.findById(clientReturnTask.getId()).orElseThrow();
        Assertions.assertThat(preTestClientReturnTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        clientReturnService.assignBarcodeAndFinishTask(
                barcodes,
                Map.of(), clientReturn.getExternalReturnId(),
                user,
                clientReturnTask.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

        //баркод у объекта поменялся
        assertThat(clientReturn.getBarcode()).isEqualTo(barcodes.get(0));

        //есть запись в очереди для отправки прикрепления баркода к возврату
        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_ATTACH_BARCODE, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CLIENT_RETURN_ATTACH_BARCODE);

        //пробовали запросить изменение
        Mockito.verify(returnsApi, Mockito.atLeastOnce()).updateReturnWithHttpInfo(any(), any());

        //не отправляли ничего в лес
        Mockito.verify(jmsTemplate, Mockito.never()).convertAndSend(anyString(), any(Event.class));

        //не появился ивент на отправку в лес
        dbQueueTestUtil.isEmpty(QueueType.CREATE_CLIENT_RETURN);

        // Возврат все равно поменял статус, т.к. по факту он у курьера
        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.RECEIVED);
        //после завершения таска закрылась
        var postTestClientReturnTask = orderDeliveryTaskRepository.findById(clientReturnTask.getId()).orElseThrow();
        assertThat(postTestClientReturnTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED);
    }

    @Test
    @DisplayName("При сдаче возврата на СЦ поменялся статус на ДОСТАВЛЕН")
    void clientReturnWasDeliveredToSc() {
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );

        clientReturnService.assignBarcodeAndFinishTask(
                barcodes,
                Map.of(), clientReturn.getExternalReturnId(),
                user,
                clientReturnTask.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

        dbQueueTestUtil.executeAllQueueItems(QueueType.CLIENT_RETURN_ATTACH_BARCODE);

        var userShift = userShiftRepository.findByUserAndActiveIsTrue(user).orElseThrow();

        testUserHelper.finishFullReturnAtEnd(userShift.getId());

        userShift = userShiftRepository.findByUserAndActiveIsTrue(user).orElseThrow();
        testUserHelper.finishUserShift(userShift.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.DELIVERED_TO_SC);
    }

    @Test
    @DisplayName("Курьер не сдал возврат на сц")
    void clientReturnWasNotDeliveredToSc() {
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );

        clientReturnService.assignBarcodeAndFinishTask(
                barcodes,
                Map.of(), clientReturn.getExternalReturnId(),
                user,
                clientReturnTask.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

        dbQueueTestUtil.executeAllQueueItems(QueueType.CLIENT_RETURN_ATTACH_BARCODE);

        var userShift = userShiftRepository.findByUserAndActiveIsTrue(user).orElseThrow();

        assert userShift.getCurrentRoutePoint() != null;
        testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());

        transactionTemplate.executeWithoutResult(ts -> {
            var userShiftTs = userShiftRepository.findByUserAndActiveIsTrue(user).orElseThrow();
            testUserHelper.startAndFinishScanClientReturnsForReturn(userShiftTs, List.of(),
                    List.of(clientReturn.getId()));
            var routePoint = userShiftTs.streamReturnRoutePoints().findFirst().orElseThrow();
            var task = routePoint.streamReturnTasks().findFirst().orElseThrow();
            commandService.finishReturnTask(userShiftTs.getUser(),
                    new UserShiftCommand.FinishReturnTask(userShiftTs.getId(),
                            routePoint.getId(), task.getId()));
            testUserHelper.finishUserShift(userShiftTs.getId());
        });


        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

        // курьер не сдал возврат
        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.RECEIVED);

        // Очередь на создание тикета в ст
        dbQueueTestUtil.assertQueueHasSize(QueueType.PARTIAL_RETURN_BOXES_CREATE_TRACKER_ISSUE, 1);
    }

    @Test
    @DisplayName("Проверка, что неверный клиентский возврат выбрасывает ошибку")
    void validateClientReturnBarcode() {
        var incorrectBarcode = List.of(INCORRECT_PREFIX + UUID.randomUUID());

        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );


        assertThatThrownBy(() ->
                clientReturnService.assignBarcodeAndFinishTask(
                        incorrectBarcode,
                        Map.of(), clientReturn.getExternalReturnId(),
                        user,
                        clientReturnTask.getId())
        ).hasMessageContaining(incorrectBarcode.get(0));
    }

    @Test
    @DisplayName("Проверка, что дважды отсканированный клиентский возврат выбрасывает ошибку")
    void validateScannedTwice() {
        var barcode = ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX + "123456";
        var multipleBarodes = List.of(barcode, barcode);
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );
        assertThatThrownBy(() ->
                clientReturnService.assignBarcodeAndFinishTask(
                        multipleBarodes,
                        Map.of(), clientReturn.getExternalReturnId(),
                        user,
                        clientReturnTask.getId())
        ).hasMessageContaining(multipleBarodes.get(0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Проверка, что повторный вызов метода завершения таски не выдаст исключение")
    void validateIdempotencyOnFinishedTask(boolean idempotencyCheckDisabled) {
        Mockito.when(
                        configurationProviderAdapter.isBooleanEnabled(FINISH_CLIENT_RETURN_TASK_IDEMPOTENCY_CHECK_DISABLED))
                .thenReturn(idempotencyCheckDisabled);
        var barcode = ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX + "123456";
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );
        clientReturnService.assignBarcodeAndFinishTask(
                List.of(barcode),
                Map.of(), clientReturn.getExternalReturnId(),
                user,
                clientReturnTask.getId());

        if (idempotencyCheckDisabled) {
            assertThatThrownBy(() ->
                    clientReturnService.assignBarcodeAndFinishTask(
                            List.of(barcode),
                            Map.of(), clientReturn.getExternalReturnId(),
                            user,
                            clientReturnTask.getId())
            ).hasMessageContaining(barcode);
        } else {
            assertDoesNotThrow(() -> clientReturnService.assignBarcodeAndFinishTask(
                    List.of(barcode),
                    Map.of(), clientReturn.getExternalReturnId(),
                    user,
                    clientReturnTask.getId()));
            assertThatThrownBy(() ->
                    clientReturnService.assignBarcodeAndFinishTask(
                            List.of(barcode + "123"),
                            Map.of(), clientReturn.getExternalReturnId(),
                            user,
                            clientReturnTask.getId())
            ).hasMessageContaining(barcode);
        }
    }

    @Test
    @DisplayName("Проверка, что при попытке прикрепить уже использованный баркод выбрасывается ошибка")
    void validateReusedBarcode() {
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );

        clientReturnService.assignBarcodeAndFinishTask(
                barcodes,
                Map.of(), clientReturn.getExternalReturnId(),
                user,
                clientReturnTask.getId());


        var otherUser = testUserHelper.findOrCreateUser(2L);
        var otherShift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var otherClientReturn = clientReturnGenerator.generateReturnFromClient();
        var createCommand = UserShiftCommand.Create.builder()
                .userId(otherUser.getId())
                .shiftId(otherShift.getId())
                .routePoint(helper.clientReturn("addr4", 13, otherClientReturn.getId()))
                .build();
        var userShiftId = userShiftTestHelper.start(createCommand);
        UserShift userShift = transactionTemplate.execute(
                cmd -> {
                    var temp = userShiftRepository.findByIdOrThrow(userShiftId);
                    Hibernate.initialize(temp.streamRoutePoints().collect(Collectors.toList()));
                    return temp;
                }
        );
        assert userShift != null;
        var otherClientReturnTask =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getClientReturnId(), otherClientReturn.getId()))
                        .findFirst()
                        .orElseThrow();

        assertThatThrownBy(() ->
                clientReturnService.assignBarcodeAndFinishTask(
                        barcodes,
                        Map.of(), otherClientReturn.getExternalReturnId(),
                        otherUser,
                        otherClientReturnTask.getId())
        ).hasMessageContaining(barcodes.get(0));
    }

    @Test
    @DisplayName("Проверка вызова метода PreciseGeoPointService::getPreciseGeoPoint при создании клиентского возврата")
    void verifyCallGetPreciseGeoPoint() {
        GeoPoint geoPoint = GeoPoint.ofLatLon(57.736708, 35.589498);
        Mockito.when(preciseGeoPointService.getPreciseGeoPoint(argThat(arg ->
                        arg.getLogisticRequestPointFrom().getGeoPoint().equals(geoPoint)),
                eq(true))).thenReturn(null);

        clientReturnGenerator.createClientReturn(getClientReturnGenerateParam(geoPoint));

        assertThat(dbQueueTestUtil.isEmpty(CLIENT_RETURN_VERIFY_COORDS)).isFalse();
        dbQueueTestUtil.executeAllQueueItems(CLIENT_RETURN_VERIFY_COORDS);

        Mockito.verify(preciseGeoPointService).getPreciseGeoPoint(argThat(arg ->
                        arg.getLogisticRequestPointFrom().getGeoPoint().equals(geoPoint)),
                eq(true));
    }

    @Test
    @DisplayName("Проверка обновления geoPoint при создании клиентского возврата")
    void updatePreciseGeoPointClientReturn() {
        GeoPoint geoPoint = GeoPoint.ofLatLon(57.736708, 35.589498);
        GeoPoint preciseGeoPoint = GeoPoint.ofLatLon(55.736708, 37.589498);
        Mockito.when(preciseGeoPointService.getPreciseGeoPoint(any(), eq(true)))
                .thenReturn(preciseGeoPoint);

        ClientReturn clientReturn = clientReturnGenerator.createClientReturn(getClientReturnGenerateParam(geoPoint));

        assertThat(dbQueueTestUtil.isEmpty(CLIENT_RETURN_VERIFY_COORDS)).isFalse();
        dbQueueTestUtil.executeAllQueueItems(CLIENT_RETURN_VERIFY_COORDS);

        ClientReturn updatedClientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(updatedClientReturn.getLogisticRequestPointFrom().getGeoPoint()).isEqualTo(preciseGeoPoint);
    }

    @Test
    void shouldThrowExceptionBecauseOfTheSkippedReturns() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CLIENT_RETURN_SCAN_ON_SC_VALIDATION_ENABLED)).thenReturn(true);
        var lockerClientReturn = clientReturnGenerator.generate();
        clientReturnService.assignBarcodeAndFinishTask(
                barcodes,
                Map.of(), clientReturn.getExternalReturnId(),
                user,
                clientReturnTask.getId());
        var scanReq = ScanRequest.builder()
                .skippedClientReturns(List.of(lockerClientReturn.getId(), clientReturn.getId()))
                .build();

        assertThrows(TplInvalidActionException.class,
                () -> clientReturnService.validateCourierClientReturnScanRequest(scanReq));
    }

    @Test
    void shouldNotThrowExceptionBecauseTheSkippedReturnIsLocker() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CLIENT_RETURN_SCAN_ON_SC_VALIDATION_ENABLED)).thenReturn(true);
        var lockerClientReturn = clientReturnGenerator.generate();
        var scanReq = ScanRequest.builder()
                .skippedClientReturns(List.of(lockerClientReturn.getId()))
                .build();

        assertDoesNotThrow(() -> clientReturnService.validateCourierClientReturnScanRequest(scanReq));
    }

    @Test
    void shouldNotThrowExceptionBecauseReturnWasNotAccepted() {
        var scanReq = ScanRequest.builder()
                .skippedClientReturns(List.of(clientReturn.getId()))
                .build();

        assertDoesNotThrow(() -> clientReturnService.validateCourierClientReturnScanRequest(scanReq));
    }

    @Test
    void shouldAddCrToCreateOwTicketsQueue() {
        var cr1 = clientReturnGenerator.generateReturnFromClient();
        var cr2 = clientReturnGenerator.generateReturnFromClient();

        clientReturnService.createOwTicket(List.of(cr1.getExternalReturnId(), cr2.getExternalReturnId(),
                cr2.getExternalReturnId()));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 2);
    }

    @Test
    void shouldNotAddCrToCreateOwTicketsQueueBecauseCouldNotFindClientReturn() {
        var cr1 = clientReturnGenerator.generateReturnFromClient();

        var ex = assertThrows(
                TplIllegalArgumentException.class,
                () -> clientReturnService.createOwTicket(List.of(cr1.getExternalReturnId(), "fake_id"))
        );
        assertThat(ex.getMessage()).contains("Could not find client returns with external ids");
        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 0);
    }

    @Test
    void shouldCloseCallTask() {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        clientReturnService.assignBarcodeAndFinishTask(
                barcodes,
                Map.of(),
                clientReturn.getExternalReturnId(),
                user,
                tod.getId());

        tod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(tod.getCallToRecipientTask().getStatus()).isEqualTo(CLOSED);
    }

    ClientReturnGenerator.ClientReturnGenerateParam getClientReturnGenerateParam(GeoPoint geoPoint) {
        var routePointRequest = CreateClientReturnRoutePointRequestDto.builder()
                .city("Moscow")
                .street("Tverskaya")
                .house("1")
                .expectedArriveTime(LocalDateTime.now(clock))
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.TEN)
                .build();

        return ClientReturnGenerator.ClientReturnGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPoint.ofLatLon(geoPoint.getLatitude(),
                                geoPoint.getLongitude()))
                        .build())
                .itemCount(Optional.ofNullable(routePointRequest.getItemCount()).orElse(2L))
                .build();
    }
}
