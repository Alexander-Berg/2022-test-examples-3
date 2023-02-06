package ru.yandex.market.tpl.core.domain.ds;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.Address;
import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.LegalEntity;
import ru.yandex.market.logistic.api.model.common.LegalForm;
import ru.yandex.market.logistic.api.model.common.Location;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.Movement;
import ru.yandex.market.logistic.api.model.common.OutboundType;
import ru.yandex.market.logistic.api.model.common.Party;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.Status;
import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.logistic.api.model.delivery.Intake;
import ru.yandex.market.logistic.api.model.delivery.request.CancelMovementRequest;
import ru.yandex.market.logistic.api.model.delivery.request.PutMovementRequest;
import ru.yandex.market.logistic.api.model.delivery.response.GetMovementResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetMovementStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.PutMovementResponse;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementCommandService;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.movement.event.MovementStatusChangedEvent;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.util.MovementControlApiConverter;
import ru.yandex.market.tpl.core.exception.TplInvalidTransitionException;
import ru.yandex.market.tpl.core.service.delivery.LogisticApiRequestProcessingConfiguration;
import ru.yandex.market.tpl.core.service.delivery.ds.DsRequestReader;
import ru.yandex.market.tpl.core.service.delivery.ds.request.CreateIntakeRequest;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.transport.Transport;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.api.model.movement.MovementStatus.CANCELLED;
import static ru.yandex.market.tpl.api.model.movement.MovementStatus.CREATED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_CANCELLED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_PREDICT_VOLUME_UPDATED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CANCEL_MOVEMENT_WHERE_VOLUME_IS_ZERO;
import static ru.yandex.market.tpl.core.domain.movement.MovementGenerator.DEFAULT_FROM_TIME;
import static ru.yandex.market.tpl.core.domain.movement.MovementGenerator.DEFAULT_TO_TIME;

/**
 * @author hauu
 */
@RequiredArgsConstructor

@ContextConfiguration(classes = {
        LogisticApiRequestProcessingConfiguration.class
})
class DsApiMovementTest extends TplAbstractTest {

    private static final String FROM_LOCATION_WAREHOUSE_YANDEX_ID = "20";

    private final DsMovementManager dsMovementManager;
    private DeliveryService partner;
    private User user;
    private Transport transport;

    private final TransactionTemplate transactionTemplate;

    private final PartnerRepository<DeliveryService> partnerRepository;
    private final MovementRepository movementRepository;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final PickupPointRepository pickupPointRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MovementCommandService movementCommandService;
    private final TestUserHelper userHelper;
    private final MovementControlApiConverter movementControlApiConverter;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final MovementGenerator movementGenerator;
    private final CacheManager oneMinuteCacheManager;
    private final Clock clock;
    private final DsRequestReader dsRequestReader;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void setUp() {
        partner = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
        user = userHelper.findOrCreateUser(35236L);
        transport = userHelper.findOrCreateTransport();
        prepareMovement(new ResourceId("TMM1", null), BigDecimal.ONE);
        configurationServiceAdapter.insertValue(CANCEL_MOVEMENT_WHERE_VOLUME_IS_ZERO, true);
    }


    @Test
    void createIntake() throws Exception {
        CreateIntakeRequest request = dsRequestReader.readRequest("/ds/create_intake.xml", CreateIntakeRequest.class);
        Intake intakeRequest = request.getIntake();
        var intakeResponse = dsMovementManager.createIntake(intakeRequest, partner);
        assertThat(intakeResponse).isNotNull();

        transactionTemplate.execute(tt -> {
            var movementO = movementRepository.findByExternalId(intakeRequest.getIntakeId().getYandexId());
            assertThat(movementO).isNotEmpty();
            ru.yandex.market.tpl.core.domain.movement.Movement movement = movementO.get();
            assertThat(movement.getStatus()).isEqualTo(MovementStatus.CREATED);
            assertThat(movement.getDeliveryIntervalFrom()).isEqualTo(intakeRequest.getTime().getFrom().toInstant());
            assertThat(movement.getDeliveryIntervalTo()).isEqualTo(intakeRequest.getTime().getTo().toInstant());
            assertThat(movement.getDeliveryServiceId()).isEqualTo(partner.getId());
            OrderWarehouse warehouse = movement.getWarehouse();
            assertThat(warehouse.getAddress().getLatitude())
                    .isEqualByComparingTo(intakeRequest.getWarehouse().getAddress().getLat());
            assertThat(warehouse.getAddress().getLongitude())
                    .isEqualByComparingTo(intakeRequest.getWarehouse().getAddress().getLng());
            assertThat(warehouse.getSchedules()).hasSize(7);

            return null;
        });
    }

    @Test
    void createIntakeFiltered() throws Exception {
        Mockito.when(configurationProviderAdapter.getValue(ConfigurationProperties.KNOWN_WAREHOUSE_YANDEX_IDS))
                .thenReturn(Optional.of("10000010736"));
        CreateIntakeRequest request = dsRequestReader.readRequest("/ds/create_intake.xml", CreateIntakeRequest.class);
        Intake intakeRequest = request.getIntake();
        dsMovementManager.createIntake(intakeRequest, partner);
        var movementO = movementRepository.findByExternalId(intakeRequest.getIntakeId().getYandexId());
        assertThat(movementO).isNotEmpty();
        ru.yandex.market.tpl.core.domain.movement.Movement movement = movementO.get();
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.LINEHAUL_FILTERED);
    }

    @Test
    void getMovement() {
        GetMovementResponse getResponse = dsMovementManager.getMovement(new ResourceId("TMM1", null));
        assertThat(getResponse).isNotNull();
        Movement savedMovement = getResponse.getMovement();
        assertThat(savedMovement.getShipper().getLogisticPoint().getPhones()).hasSize(4); // сейчас 4 потому что
        // additional number десериализуем из бд как еще один основной
        assertThat(savedMovement.getShipper().getLogisticPoint().getContact()).isNotNull();
        assertThat(savedMovement.getShipper().getLogisticPoint().getContact().getName()).isEqualTo("Имя");
        assertThat(savedMovement.getShipper().getLogisticPoint().getContact().getSurname()).isEqualTo("Фамилия");
        assertThat(savedMovement.getShipper().getLogisticPoint().getContact().getPatronymic()).isNull();
        var partnerId = savedMovement.getMovementId().getPartnerId();
        assertThat(partnerId).isNotNull();
    }

    @Test
    void getMovementWithoutContact() {
        dsMovementManager.putMovement(
                Movement.builder(
                        new ResourceId("TMM11", null),
                        DateTimeInterval.fromFormattedValue("2021-03-03T20:00:00+03:00/2021-03-03T21:00:00+03:00"),
                        null
                )
                        .setWeight(null)
                        .setShipper(new Party(fromLocationWithoutContact(), legalEntity(), null))
                        .setReceiver(new Party(toLocation(), legalEntity(), null))
                        .setComment(null)
                        .setMaxPalletCapacity(10)
                        .build(),
                partner
        );

        GetMovementResponse getResponse = dsMovementManager.getMovement(new ResourceId("TMM11", null));
        assertThat(getResponse).isNotNull();
        Movement savedMovement = getResponse.getMovement();
        assertThat(savedMovement.getShipper().getLogisticPoint().getPhones()).hasSize(0);
        assertThat(savedMovement.getShipper().getLogisticPoint().getContact()).isNull();
    }

    @Test
    void putMovementCreate() {
        Optional<ru.yandex.market.tpl.core.domain.movement.Movement> movementO =
                movementRepository.findByExternalId("TMM1");
        assertThat(
                movementO
                        .map(ru.yandex.market.tpl.core.domain.movement.Movement::getId)
                        .orElseThrow(() -> new RuntimeException("Test failed, no movement with TMM1 id saved"))
        ).isNotNull();

    }

    @Test
    void putMovement_createPickupPoint() throws Exception {
        String movementId = "TMM315623";
        PutMovementRequest request = dsRequestReader.readRequest("/ds/put_movement.xml", PutMovementRequest.class);
        dsMovementManager.putMovement(request.getMovement(), partner);
        Optional<PickupPoint> pickupPoint = pickupPointRepository.findByLogisticPointId(
                Long.parseLong(request
                        .getMovement()
                        .getShipper()
                        .getLogisticPoint()
                        .getLogisticPointId()
                        .getYandexId())
        );
        assertThat(pickupPoint.isPresent()).isTrue();
        pickupPoint = pickupPointRepository.findByLogisticPointId(
                Long.parseLong(request
                        .getMovement()
                        .getReceiver()
                        .getLogisticPoint()
                        .getLogisticPointId()
                        .getYandexId())
        );
        assertThat(pickupPoint.isPresent()).isTrue();
    }

    @Test
    void putMovement_persistsTags() throws Exception {
        String movementId = "TMM315623";
        var tags = List.of(OutboundType.DROPOFF_RETURN);

        PutMovementRequest request = dsRequestReader.readRequest("/ds/put_movement.xml", PutMovementRequest.class);
        dsMovementManager.putMovement(request.getMovement(), partner);

        Optional<ru.yandex.market.tpl.core.domain.movement.Movement> movementPersistedOpt =
                movementRepository.findByExternalId(movementId);
        assertThat(movementPersistedOpt.isPresent()).isTrue();

        ru.yandex.market.tpl.core.domain.movement.Movement movementPersisted = movementPersistedOpt.get();
        assertThat(movementPersisted.getTags()).containsAll(
                tags.stream()
                        .map(OutboundType::getCode)
                        .map(Object::toString)
                        .collect(Collectors.toList())
        );
    }

    @Test
    void putMovement_notFilteredForDropOffReturn() throws Exception {
        Mockito.when(configurationProviderAdapter.getValue(ConfigurationProperties.KNOWN_WAREHOUSE_YANDEX_IDS))
                .thenReturn(Optional.of("10000985974"));
        String movementId = "TMM315623";

        PutMovementRequest request = dsRequestReader.readRequest("/ds/put_movement.xml", PutMovementRequest.class);
        dsMovementManager.putMovement(request.getMovement(), partner);

        Optional<ru.yandex.market.tpl.core.domain.movement.Movement> movementPersistedOpt =
                movementRepository.findByExternalId(movementId);
        assertThat(movementPersistedOpt.isPresent()).isTrue();

        ru.yandex.market.tpl.core.domain.movement.Movement movementPersisted = movementPersistedOpt.get();
        assertThat(movementPersisted.getStatus()).isEqualTo(CREATED);
        Mockito.reset(configurationProviderAdapter);
    }

    @Test
    void putMovementUpdate() {

        Long originalMovementId = movementRepository.findByExternalId("TMM1")
                .map(ru.yandex.market.tpl.core.domain.movement.Movement::getId)
                .orElseThrow(() -> new RuntimeException("Test failed, no movement with TMM1 id saved"));

        assertThat(originalMovementId).isNotNull();

        PutMovementResponse updatedMovement = dsMovementManager.putMovement(
                Movement.builder(
                        new ResourceId("TMM1", null),
                        DateTimeInterval.fromFormattedValue("2021-03-03T20:00:00+03:00/2021-03-03T21:00:00+03:00"),
                        BigDecimal.TEN
                )
                        .setWeight(null)
                        .setShipper(new Party(fromLocation(), legalEntity(), null))
                        .setReceiver(new Party(toLocation(), legalEntity(), null))
                        .setComment(null)
                        .setMaxPalletCapacity(null)
                        .build(),
                partner
        );
        assertThat(originalMovementId).isEqualTo(Long.valueOf(updatedMovement.getMovementId().getPartnerId()));

        assertThat(
                movementRepository.findByExternalId("TMM1")
                        .map(ru.yandex.market.tpl.core.domain.movement.Movement::getVolume)
                        .orElseThrow(() -> new RuntimeException("Test failed, no movement with TMM1 id saved"))
        ).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void updateWarehouseAddressAfterPutMovement() {
        dbQueueTestUtil.assertQueueHasSize(QueueType.UPDATE_WAREHOUSE_ADDRESS, 0);

        OrderWarehouse warehouse =
                orderWarehouseRepository.findFirstByYandexIdOrderBySchedulesIdAsc(FROM_LOCATION_WAREHOUSE_YANDEX_ID).orElseThrow();

        assertThat(warehouse.getContact()).isEqualTo("Имя Фамилия ");
        assertThat(warehouse.getAddress().getAddress())
                .isEqualTo("Новинский бульвар, д. 8, подъезд -, этаж 5, домофон ");

        LogisticPoint fromLocation = LogisticPoint.builder(new ResourceId(FROM_LOCATION_WAREHOUSE_YANDEX_ID, null))
                .setLocation(new Location(
                        "Россия",
                        "ЦФО",
                        "Москва",
                        "Москва",
                        "Москва",
                        "",
                        "Льва Толстого",
                        "16",
                        null,
                        null,
                        null,
                        "101000",
                        "-",
                        5,
                        "Парк культуры",
                        BigDecimal.valueOf(55.751310),
                        BigDecimal.valueOf(37.584613),
                        213L,
                        ""
                ))
                .setPhones(List.of(
                        new Phone("88005553535", "123"),
                        new Phone("123456789", null),
                        new Phone(null, "88005553535"),
                        new Phone(null, null)
                ))
                .setContact(new Person("Имя", "Фамилия", "Отчество"))
                .build();
        dsMovementManager.putMovement(
                Movement.builder(
                        new ResourceId("TMM2", null),
                        DateTimeInterval.fromFormattedValue("2021-03-03T20:00:00+03:00/2021-03-03T21:00:00+03:00"),
                        BigDecimal.TEN
                )
                        .setWeight(null)
                        .setShipper(new Party(fromLocation, legalEntity(), null))
                        .setReceiver(new Party(toLocation(), legalEntity(), null))
                        .setComment(null)
                        .setMaxPalletCapacity(null)
                        .build(),
                partner
        );

        assertThat(
                movementRepository.findByExternalId("TMM2")
                        .map(ru.yandex.market.tpl.core.domain.movement.Movement::getVolume)
                        .orElseThrow(() -> new RuntimeException("Test failed, no movement with TMM2 id saved"))
        ).isEqualTo(BigDecimal.TEN);

        dbQueueTestUtil.assertQueueHasSize(QueueType.UPDATE_WAREHOUSE_ADDRESS, 2);

        dbQueueTestUtil.executeAllQueueItems(QueueType.UPDATE_WAREHOUSE_ADDRESS);

        OrderWarehouse updatedWarehouse =
                orderWarehouseRepository.findFirstByYandexIdOrderBySchedulesIdAsc(FROM_LOCATION_WAREHOUSE_YANDEX_ID).orElseThrow();

        assertThat(updatedWarehouse.getContact()).isEqualTo("Имя Фамилия Отчество");
        assertThat(updatedWarehouse.getAddress().getAddress())
                .isEqualTo("Льва Толстого, д. 16, подъезд -, этаж 5, домофон ");
    }

    @Test
    void getMovementStatus() {
        var status = dsMovementManager.getMovementStatus(List.of(new ResourceId("TMM1", null)));
        assertThat(status.getMovementStatuses().get(0).getStatus().getStatusCode()).isEqualTo(StatusCode.CREATED);
    }

    @Test
    void getMovementConfirmedStatus() {
        var movement = movementRepository.findByExternalId("TMM1").get();
        movementCommandService.confirm(new MovementCommand.Confirm(movement.getId()));
        movementHistoryEventRepository.flush();
        var status = dsMovementManager.getMovementStatusHistory(List.of(new ResourceId("TMM1", null)));
        assertThat(getCodes(status)).isEqualTo(List.of(StatusCode.CREATED, StatusCode.MOVEMENT_CONFIRMED));
    }

    @Test
    void getMovementUnknownStatus() {
        var movement = movementRepository.findByExternalId("TMM1").get();
        MovementHistoryEvent event =  MovementHistoryEvent.builder()
                .movementId(movement.getId())
                .source(Source.DELIVERY)
                .type(MovementHistoryEventType.MOVEMENT_REOPENED)
                .date(Instant.now())
                .build();
        movementHistoryEventRepository.save(event);
        movementHistoryEventRepository.flush();

        var status = dsMovementManager.getMovementStatus(List.of(new ResourceId("TMM1", null)));
        assertThat(status.getMovementStatuses().get(0).getStatus().getStatusCode()).isNotEqualTo(StatusCode.UNKNOWN);
    }


    @Test
    void getMovementStatusHistoryUnknownStatus() {
        var movement = movementRepository.findByExternalId("TMM1").get();
        MovementHistoryEvent event =  MovementHistoryEvent.builder()
                .movementId(movement.getId())
                .source(Source.DELIVERY)
                .type(MovementHistoryEventType.MOVEMENT_REOPENED)
                .date(Instant.now())
                .build();
        movementHistoryEventRepository.save(event);
        movementHistoryEventRepository.flush();
        var status = dsMovementManager.getMovementStatusHistory(List.of(new ResourceId("TMM1", null)));
        assertThat(getCodes(status)).doesNotContain(StatusCode.UNKNOWN);
    }

    @Test
    void getMovementStatusHistory() {
        var status = dsMovementManager.getMovementStatusHistory(List.of(new ResourceId("TMM1", null)));
        assertThat(getCodes(status)).isEqualTo(List.of(StatusCode.CREATED));

        dsMovementManager.cancelMovement(new CancelMovementRequest(new ResourceId("TMM1", null)));
        movementHistoryEventRepository.flush();
        status = dsMovementManager.getMovementStatusHistory(List.of(new ResourceId("TMM1", null)));
        assertThat(getCodes(status)).isEqualTo(List.of(StatusCode.CREATED, StatusCode.CANCELLED));
    }

    private List<StatusCode> getCodes(GetMovementStatusHistoryResponse response) {
        return response.getMovementStatusHistories().get(0).getHistory()
                .stream()
                .map(Status::getStatusCode).collect(Collectors.toList());
    }

    @Test
    void cancelMovementByTMExpectedOk() {
        dsMovementManager.cancelMovement(new CancelMovementRequest(new ResourceId("TMM1", null)));
        var status = dsMovementManager.getMovementStatus(List.of(new ResourceId("TMM1", null)));
        assertThat(status.getMovementStatuses().get(0).getStatus().getStatusCode()).isEqualTo(StatusCode.CANCELLED);
    }

    @Test
    void cancelMovementByPartnerExpectedOk() {
        transactionTemplate.execute(ts -> {
            movementRepository.findByExternalId("TMM1")
                    .ifPresent(m -> m.cancel(Source.OPERATOR, "прост)0)").forEach(eventPublisher::publishEvent));
            return null;
        });

        var status = dsMovementManager.getMovementStatus(List.of(new ResourceId("TMM1", null)));
        assertThat(status.getMovementStatuses()
                .get(0)
                .getStatus()
                .getStatusCode()).isEqualTo(StatusCode.CANCELLED_BY_PARTNER);
    }

    @Test
    void cancelMovementExpectedNotOk() {
        setMovementStatus(MovementStatus.TRANSPORTATION_TO_SC);
        assertThatThrownBy(() -> {
            dsMovementManager.cancelMovement(new CancelMovementRequest(new ResourceId("TMM1", null)));
        }).isInstanceOf(TplInvalidTransitionException.class);
        setMovementStatus(MovementStatus.DELIVERED_TO_SC);
        assertThatThrownBy(() -> {
            dsMovementManager.cancelMovement(new CancelMovementRequest(new ResourceId("TMM1", null)));
        }).isInstanceOf(TplInvalidTransitionException.class);
    }

    @Test
    void getCourierMovement() {
        Courier courier = movementControlApiConverter.toExternal(user, null);

        assertThat(courier.getCar().getNumber()).isEqualTo(user.getVehicleNumber());
        assertThat(courier.getCar().getDescription()).isEqualTo(String.valueOf(user.getUid()));
        assertThat(courier.getPersons().get(0).getName()).isEqualTo(user.getFirstName());
        assertThat(courier.getPersons().get(0).getSurname()).isEqualTo(user.getLastName());
        assertThat(courier.getLegalEntity().getName()).isEqualTo(user.getCompany().getName());
    }

    @Test
    void getCourierMovementWithTransport() {
        Courier courier = movementControlApiConverter.toExternal(user, transport);

        assertThat(courier.getCar().getNumber()).isEqualTo(transport.getNumber());
        assertThat(courier.getCar().getDescription()).isEqualTo(String.valueOf(user.getUid()));
        assertThat(courier.getPersons().get(0).getName()).isEqualTo(user.getFirstName());
        assertThat(courier.getPersons().get(0).getSurname()).isEqualTo(user.getLastName());
        assertThat(courier.getLegalEntity().getName()).isEqualTo(user.getCompany().getName());
    }

    @Test
    void cancelMovement_WhenVolumeIsZero() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CANCEL_MOVEMENT_WHERE_VOLUME_IS_ZERO))
                .thenReturn(true);

        prepareMovement(new ResourceId("TMM2", null), BigDecimal.ZERO);

        var movement = movementRepository.findByExternalId("TMM2").get();
        assertThat(movement.getStatus()).isEqualTo(CANCELLED);

        List<MovementHistoryEvent> cancelledMovementHistoryEvents =
                movementHistoryEventRepository.findByMovementId(movement.getId(), Pageable.unpaged())
                        .stream()
                        .filter(event -> event.getType() == MOVEMENT_CANCELLED)
                        .collect(Collectors.toList());

        assertThat(cancelledMovementHistoryEvents).hasSize(1);
        assertThat(cancelledMovementHistoryEvents.get(0).getContext()).isEqualTo("Объем груза равен 0.");
    }

    @Test
    void saveShipper() {
        var movement = movementRepository.findByExternalId("TMM1").get();

        assertThat(movement.getShipper().getCompanyName()).isEqualTo("ООО Яндекс Маркет");
    }

    @Test
    void predictMovementVolumeByHistoricalData() {
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(DeliveryService.FAKE_DS_ID);

        // добавим исторических данных
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        movementGenerator.generate(
                MovementCommand.Create.builder()
                        .deliveryServiceId(deliveryService.getId())
                        .volume(BigDecimal.ONE)
                        .deliveryIntervalFrom(
                                yesterday
                                        .atTime(DEFAULT_FROM_TIME)
                                        .atZone(DateTimeUtil.DEFAULT_ZONE_ID)
                                        .toInstant())
                        .deliveryIntervalTo(
                                yesterday
                                        .atTime(DEFAULT_TO_TIME)
                                        .atZone(DateTimeUtil.DEFAULT_ZONE_ID)
                                        .toInstant())
                        .orderWarehouse(orderWarehouseRepository.findFirstByYandexIdOrderBySchedulesIdAsc("20").orElseThrow())
                        .build()
        );

        Collection<String> cacheNames = oneMinuteCacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            oneMinuteCacheManager.getCache(cacheName).clear();
        }

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                deliveryService.getSortingCenter(),
                SortingCenterProperties.SAME_DAY_DROPOFF_ENABLED,
                Boolean.TRUE
        );

        prepareMovement(new ResourceId("TMM11", null), BigDecimal.ZERO, deliveryService);

        var movement = movementRepository.findByExternalId("TMM11").get();
        assertThat(movement.getStatus()).isEqualTo(CREATED);
        assertThat(movement.getVolume()).isEqualTo(BigDecimal.ZERO);
        assertThat(movement.getPredictedVolume()).isNull();

        dbQueueTestUtil.assertQueueHasSize(QueueType.PREDICT_MOVEMENT_VOLUME, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PREDICT_MOVEMENT_VOLUME);

        var updatedMovement = movementRepository.findByExternalId("TMM11").get();
        assertThat(updatedMovement.getVolume()).isEqualTo(BigDecimal.ZERO);
        assertThat(updatedMovement.getPredictedVolume()).isEqualByComparingTo("1.5");

        List<MovementHistoryEvent> movementHistoryEvents =
                movementHistoryEventRepository.findByMovementId(movement.getId(), Pageable.unpaged())
                        .stream()
                        .filter(event -> event.getType() == MOVEMENT_PREDICT_VOLUME_UPDATED)
                        .collect(Collectors.toList());

        assertThat(movementHistoryEvents).hasSize(1);
    }

    @Test
    void predictMovementVolumeWithoutHistoricalData() {
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(DeliveryService.FAKE_DS_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                deliveryService.getSortingCenter(),
                SortingCenterProperties.SAME_DAY_DROPOFF_ENABLED,
                Boolean.TRUE
        );

        prepareMovement(new ResourceId("TMM11", null), BigDecimal.ONE, deliveryService);

        var movement = movementRepository.findByExternalId("TMM11").get();
        assertThat(movement.getStatus()).isEqualTo(CREATED);
        assertThat(movement.getVolume()).isEqualTo(BigDecimal.ONE);
        assertThat(movement.getPredictedVolume()).isNull();

        dbQueueTestUtil.assertQueueHasSize(QueueType.PREDICT_MOVEMENT_VOLUME, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PREDICT_MOVEMENT_VOLUME);

        var updatedMovement = movementRepository.findByExternalId("TMM11").get();
        assertThat(updatedMovement.getVolume()).isEqualTo(BigDecimal.ONE);
        assertThat(updatedMovement.getPredictedVolume()).isEqualByComparingTo("1.5");

        List<MovementHistoryEvent> movementHistoryEvents =
                movementHistoryEventRepository.findByMovementId(movement.getId(), Pageable.unpaged())
                        .stream()
                        .filter(event -> event.getType() == MOVEMENT_PREDICT_VOLUME_UPDATED)
                        .collect(Collectors.toList());

        assertThat(movementHistoryEvents).hasSize(1);
    }

    @Test
    void cancelAfterPredictMovementVolumeWithoutHistoricalDataAndZeroVolume() {
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(DeliveryService.FAKE_DS_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                deliveryService.getSortingCenter(),
                SortingCenterProperties.SAME_DAY_DROPOFF_ENABLED,
                Boolean.TRUE
        );

        prepareMovement(new ResourceId("TMM11", null), BigDecimal.ZERO, deliveryService);

        var movement = movementRepository.findByExternalId("TMM11").get();
        assertThat(movement.getStatus()).isEqualTo(CREATED);
        assertThat(movement.getVolume()).isEqualTo(BigDecimal.ZERO);
        assertThat(movement.getPredictedVolume()).isNull();

        dbQueueTestUtil.assertQueueHasSize(QueueType.PREDICT_MOVEMENT_VOLUME, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PREDICT_MOVEMENT_VOLUME);

        var updatedMovement = movementRepository.findByExternalId("TMM11").get();
        assertThat(updatedMovement.getStatus()).isEqualTo(CANCELLED);
        assertThat(updatedMovement.getVolume()).isEqualTo(BigDecimal.ZERO);
        assertThat(updatedMovement.getPredictedVolume()).isNull();

        List<MovementHistoryEvent> movementHistoryEvents =
                movementHistoryEventRepository.findByMovementId(movement.getId(), Pageable.unpaged())
                        .stream()
                        .filter(event -> event.getType() == MOVEMENT_CANCELLED)
                        .collect(Collectors.toList());

        assertThat(movementHistoryEvents).hasSize(1);
        assertThat(movementHistoryEvents.get(0).getContext()).isEqualTo("Объем груза равен 0");
    }

    private void setMovementStatus(MovementStatus status) {
        ru.yandex.market.tpl.core.domain.movement.Movement movement = movementRepository.findByExternalId("TMM1").get();
        movement.setStatus(status);
        movementRepository.save(movement);
    }

    private PutMovementResponse prepareMovement(ResourceId initialResourceId, BigDecimal initialVolume) {
        return prepareMovement(initialResourceId, initialVolume, partner);
    }

    private PutMovementResponse prepareMovement(
            ResourceId initialResourceId,
            BigDecimal initialVolume,
            DeliveryService deliveryService
    ) {
        return dsMovementManager.putMovement(
                Movement.builder(
                        initialResourceId,
                        DateTimeInterval.fromFormattedValue("2021-03-03T20:00:00+03:00/2021-03-03T21:00:00+03:00"),
                        initialVolume
                )
                        .setWeight(null)
                        .setShipper(new Party(fromLocation(), legalEntity(), null))
                        .setReceiver(new Party(toLocation(), legalEntity(), null))
                        .setComment(null)
                        .setMaxPalletCapacity(1)
                        .build(),
                deliveryService
        );
    }

    private LogisticPoint toLocation() {
        return LogisticPoint.builder(new ResourceId("200", null))
                .setLocation(new Location(
                        "Россия",
                        "ЦФО",
                        "Москва",
                        "Москва",
                        "Москва",
                        "",
                        "ул. Логистическая",
                        "1",
                        null,
                        null,
                        null,
                        "101000",
                        "-",
                        5,
                        "Котельники",
                        BigDecimal.valueOf(55.751310),
                        BigDecimal.valueOf(37.584613),
                        213L,
                        ""
                ))
                .build();
    }

    private LegalEntity legalEntity() {
        return new LegalEntity(
                "ООО Яндекс Маркет",
                "ООО Яндекс Маркет",
                LegalForm.OOO,
                "1167746491395",
                "7704357909",
                "770401001",
                Address.builder(
                        "121099, город Москва, Новинский бульвар, дом 8, помещение 9.03 этаж 9"
                ).build(),
                "",
                "",
                "",
                ""
        );
    }

    private LogisticPoint fromLocation() {
        return LogisticPoint.builder(new ResourceId("20", null))
                .setLocation(new Location(
                        "Россия",
                        "ЦФО",
                        "Москва",
                        "Москва",
                        "Москва",
                        "",
                        "Новинский бульвар",
                        "8",
                        null,
                        null,
                        null,
                        "101000",
                        "-",
                        5,
                        "Смоленская",
                        BigDecimal.valueOf(55.751310),
                        BigDecimal.valueOf(37.584613),
                        213L,
                        ""
                ))
                .setPhones(List.of(
                        new Phone("88005553535", "123"),
                        new Phone("123456789", null),
                        new Phone(null, "88005553535"),
                        new Phone(null, null)
                ))
                .setContact(new Person("Имя", "Фамилия", null))
                .build();
    }

    private LogisticPoint fromLocationWithoutContact() {
        return LogisticPoint.builder(new ResourceId("202", null))
                .setLocation(new Location(
                        "Россия",
                        "ЦФО",
                        "Москва",
                        "Москва",
                        "Москва",
                        "",
                        "Новинский бульвар",
                        "8",
                        null,
                        null,
                        null,
                        "101000",
                        "-",
                        5,
                        "Смоленская",
                        BigDecimal.valueOf(55.751310),
                        BigDecimal.valueOf(37.584613),
                        213L,
                        ""
                ))
                .build();
    }
}
