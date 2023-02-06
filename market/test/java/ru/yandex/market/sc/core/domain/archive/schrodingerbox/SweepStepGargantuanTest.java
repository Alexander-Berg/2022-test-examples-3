package ru.yandex.market.sc.core.domain.archive.schrodingerbox;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.archive.ArchiveQueryService;
import ru.yandex.market.sc.core.domain.archive.ArchivingSettingsService;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveRepository;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveStatus;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.location.repository.Location;
import ru.yandex.market.sc.core.domain.measurements.repository.Measurements;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.transfermanager.TransferManagerService;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.TestEntity;


@EmbeddedDbTest
public class SweepStepGargantuanTest {
    private static final HashMap<TestEntity, List<RelatedEntity>> EXPECTED_ARCHIVING_BEHAVIOR = new HashMap<>();
    private static final HashMap<TestEntity, List<TestEntity>> OVERRIDE_EXECUTION_ORDER = new HashMap<>();
    private static final Set<TestEntity> NOT_ARCHIVABLE_ENTITIES = Set.of(TestEntity.CELL);

    /**
     * Декларативное описание того как должна вести себя архивация.
     * На основании этого описания будут автоматически составлены тест кейсы
     * и ожидаемые результаты их выполнения.
     */
    static {
        // Отображение: Какую сущность тестируем (архивируем) -> какие сущности нужно-можно создать для этого тест кейса
        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.PLACE, List.of(new RelatedEntity(TestEntity.ORDERS)));
        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ORDER_SCAN_LOG, List.of(new RelatedEntity(TestEntity.ORDERS)));
        EXPECTED_ARCHIVING_BEHAVIOR.put(
                TestEntity.ORDER_FF_STATUS_HISTORY,
                List.of(new RelatedEntity(TestEntity.ORDERS)
                ));
        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ORDER_UPDATE_HISTORY, List.of(new RelatedEntity(TestEntity.ORDERS)));
        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ORDER_ITEM, List.of(new RelatedEntity(TestEntity.ORDERS)));
        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ORDER_TICKET, List.of(new RelatedEntity(TestEntity.ORDERS)));

        // temporary disabled
//        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.SORTABLE, List.of(new RelatedEntity(TestEntity.ORDERS)));

        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ROUTE_FINISH_ORDER, List.of(
                new RelatedEntity(TestEntity.ROUTE, false, false),
                new RelatedEntity(TestEntity.ROUTE_FINISH, false, false),
                new RelatedEntity(TestEntity.ORDERS)
        ));

        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ROUTE_FINISH_PLACE, List.of(
                new RelatedEntity(TestEntity.ROUTE, false, false),
                new RelatedEntity(TestEntity.ROUTE_FINISH, false, false),
                new RelatedEntity(TestEntity.ORDERS, false, true),
                new RelatedEntity(TestEntity.PLACE, false, false)
        ));

        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.PLACE_HISTORY, List.of(
                new RelatedEntity(TestEntity.ORDERS, false, false),
                new RelatedEntity(TestEntity.PLACE)
        ));

        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.PLACE_PARTNER_CODE, List.of(
                new RelatedEntity(TestEntity.ORDERS, false, false),
                new RelatedEntity(TestEntity.PLACE)
        ));

        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ROUTE_FINISH, List.of(
                new RelatedEntity(TestEntity.ORDERS, false, false),
                new RelatedEntity(TestEntity.PLACE, false, false),
                new RelatedEntity(TestEntity.ROUTE, false, false),
                new RelatedEntity(TestEntity.ROUTE_FINISH_ORDER, true),
                new RelatedEntity(TestEntity.ROUTE_FINISH_PLACE, true)));

        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ROUTE, List.of(
                new RelatedEntity(TestEntity.ROUTE_FINISH, true, true)
        ));

        EXPECTED_ARCHIVING_BEHAVIOR.put(TestEntity.ROUTE_CELL, List.of(
                new RelatedEntity(TestEntity.ROUTE, false, true),
                new RelatedEntity(TestEntity.CELL, false, false)
        ));

        // Когда порядок создания сущностей по умолчанию не подходит для тест кейса, его можно переопределить
        OVERRIDE_EXECUTION_ORDER.put(TestEntity.ROUTE_FINISH, List.of(
                TestEntity.ORDERS,
                TestEntity.PLACE,
                TestEntity.ROUTE,
                TestEntity.ROUTE_FINISH,
                TestEntity.ROUTE_FINISH_ORDER,
                TestEntity.ROUTE_FINISH_PLACE));

        OVERRIDE_EXECUTION_ORDER.put(TestEntity.ROUTE, List.of(
                TestEntity.ROUTE,
                TestEntity.ROUTE_FINISH
        ));

        OVERRIDE_EXECUTION_ORDER.put(TestEntity.ROUTE_FINISH_PLACE, List.of(
                TestEntity.ROUTE,
                TestEntity.ROUTE_FINISH,
                TestEntity.ORDERS,
                TestEntity.PLACE,
                TestEntity.ROUTE_FINISH_PLACE
        ));

        OVERRIDE_EXECUTION_ORDER.put(TestEntity.ROUTE_CELL, List.of(
                TestEntity.CELL,
                TestEntity.ROUTE,
                TestEntity.ROUTE_CELL
        ));
    }


    private static final List<ArchiveStatus> DELETABLE_ARCHIVE_STATUS = List.of(
            ArchiveStatus.VERIFICATION_FINISHED,
            ArchiveStatus.DELETE_FAILED
    );
    private static final List<ArchiveStatus> NON_DELETABLE_ARCHIVE_STATUS = Arrays.stream(ArchiveStatus.values())
            .filter(it -> !DELETABLE_ARCHIVE_STATUS.contains(it))
            .toList();


    @Autowired
    SweepStep sweepStep;

    @Autowired
    TestFactory testFactory;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ArchiveRepository archiveRepository;

    @Autowired
    ArchivingTestJdbcEntityManager jdbcEntityManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ArchivingSettingsService archivingSettingsService;

    @MockBean
    TransferManagerService transferManagerService;

    private static final long OTHER_ARCHIVE_ID = 987654321L;
    private static final long NON_DELETABLE_ARCHIVE_ID = -10L;

    private SortingCenter sortingCenter;

    private DeliveryService deliveryService;
    private Warehouse warehouse;
    private Measurements measurements;
    private Location location;
    private User user;

    @BeforeEach
    void beforeAll() {
        sortingCenter = testFactory.storedSortingCenter();
        deliveryService = testFactory.storedDeliveryService();
        user = testFactory.storedUser(sortingCenter, 1000);
        warehouse = testFactory.storedWarehouse();
        measurements = testFactory.storedMeasurements();
        location = testFactory.storedLocation();

        enableArchiving();
        enableArchivingDelete();
    }

    private static List<SweepTestParams> testParams() {
        List<SweepTestParams> testCases = new ArrayList<>();

        createTestCasesForWholeGraph(testCases);
        for (TestEntity testEntity : EXPECTED_ARCHIVING_BEHAVIOR.keySet()) {
            createTCforDeleteGen(testCases, testEntity);
        }

        System.out.println("Total test cases: " + testCases.size());
        return testCases;
    }

    private static void createTestCasesForWholeGraph(List<SweepTestParams> testCases) {
        final List<TestEntity> values = List.of(
                TestEntity.ORDERS,
                TestEntity.ORDER_SCAN_LOG,
                TestEntity.ORDER_FF_STATUS_HISTORY,
                TestEntity.ORDER_ITEM,
                TestEntity.ORDER_TICKET,
                TestEntity.ORDER_UPDATE_HISTORY,
                TestEntity.PLACE,
                TestEntity.ROUTE,
                TestEntity.CELL,
                TestEntity.ROUTE_CELL,
                TestEntity.ROUTE_FINISH,
                TestEntity.ROUTE_FINISH_ORDER,
                TestEntity.ROUTE_FINISH_PLACE,
                TestEntity.PLACE_HISTORY,
                TestEntity.PLACE_PARTNER_CODE
        );

        final List<Pair<TestEntity, Boolean>> pairStream = values.stream().map(te -> new Pair<>(te, true))
                .toList();
        for (var deletableArchiveStatus : DELETABLE_ARCHIVE_STATUS) {
            for (var nonDeletableArchiveStatus : NON_DELETABLE_ARCHIVE_STATUS) {

                SweepTestParams params = SweepTestParams.builder()
                        .testId("1-1 Archives with status " + nonDeletableArchiveStatus
                                + " shouldn't be deleted. Archives with status "
                                + deletableArchiveStatus + " should.")
                        .entityToBuildGraphFrom(null)
                        .otherEntities(pairStream)
                        .overrideExecutionOrder(values)
                        .useNotDeletableArchive(true)
                        .nonDeletableArchiveStatus(nonDeletableArchiveStatus.name())
                        .deletableArchiveStatus(deletableArchiveStatus.name())
                        .build();

                testCases.add(params);
            }
            SweepTestParams params = SweepTestParams.builder()
                    .testId("1-2 record with null archives shouldn't be deleted." +
                            " Records with appropriate archive should.")
                    .entityToBuildGraphFrom(null)
                    .otherEntities(pairStream)
                    .overrideExecutionOrder(values)
                    .useNotDeletableArchive(false)
                    .deletableArchiveStatus(deletableArchiveStatus.name())
                    .build();

            testCases.add(params);
        }

    }

    private static void createTCforDeleteGen(List<SweepTestParams> testCases, TestEntity entityToBuildGraphFrom) {
        final List<TestEntity> values = EXPECTED_ARCHIVING_BEHAVIOR.get(entityToBuildGraphFrom).stream()
                .map(RelatedEntity::testEntity).toList();

        final List<Pair<TestEntity, Boolean>> entitiesToCreate =
                new ArrayList<>(values.stream().map(te -> new Pair<>(te, true)).toList());
        entitiesToCreate.add(new Pair<>(entityToBuildGraphFrom, true));

        for (var deletableArchiveStatus : DELETABLE_ARCHIVE_STATUS) {
            for (var nonDeletableArchiveStatus : NON_DELETABLE_ARCHIVE_STATUS) {
                SweepTestParams params = SweepTestParams.builder()
                        .testId("2-1 Archives for entity graph for entity " + entityToBuildGraphFrom.name() +
                                " with status " + nonDeletableArchiveStatus
                                + " shouldn't be deleted. Archives with status "
                                + deletableArchiveStatus + " should.")
                        .entityToBuildGraphFrom(entityToBuildGraphFrom)
                        .otherEntities(entitiesToCreate)
                        .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(entityToBuildGraphFrom))
                        .useNotDeletableArchive(true)
                        .deletableArchiveStatus(deletableArchiveStatus.name())
                        .nonDeletableArchiveStatus(nonDeletableArchiveStatus.name())
                        .build();

                testCases.add(params);
            }

            SweepTestParams params = SweepTestParams.builder()
                    .testId("2-2 Records for entity graph for entity " + entityToBuildGraphFrom.name() +
                            " shouldn't be deleted if archive is not exist. Archives with status "
                            + deletableArchiveStatus + " should.")
                    .entityToBuildGraphFrom(entityToBuildGraphFrom)
                    .otherEntities(entitiesToCreate)
                    .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(entityToBuildGraphFrom))
                    .useNotDeletableArchive(false)
                    .deletableArchiveStatus(deletableArchiveStatus.name())
                    .build();

            testCases.add(params);
        }
    }


    @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
    @Transactional
    @ParameterizedTest
    @MethodSource("testParams")
    @Disabled("broken")
    void checkIfRecordsArchivedProperly(SweepTestParams params) {
        System.out.println("test id = " + params.testId);
        System.out.println("test params = " + params);

        //test params
        List<Pair<TestEntity, Boolean>> entitiesToCreate = params.otherEntities;
        TestEntity testEntity = params.entityToBuildGraphFrom;
        final Boolean useNotDeletableArchive = params.useNotDeletableArchive;

        final Long notDeletableArchiveId;
        if (useNotDeletableArchive != null && useNotDeletableArchive) {
            notDeletableArchiveId = NON_DELETABLE_ARCHIVE_ID;
        } else {
            notDeletableArchiveId = null;
        }

        final List<Triple<TestEntity, Long, Boolean>> archivedEntities = entitiesToCreate.stream().map(etc ->
                new Triple<TestEntity, Long, Boolean>(
                        etc.first,
                        null,
                        etc.second
                )).toList();
        final List<Triple<TestEntity, Long, Boolean>> notArchivedEntities = entitiesToCreate.stream().map(etc ->
                new Triple<TestEntity, Long, Boolean>(
                        etc.first,
                        null,
                        false
                )).toList();

        //test data preparation
        jdbcEntityManager.createArchive(-1, params.deletableArchiveStatus);
        jdbcEntityManager.createArchive(OTHER_ARCHIVE_ID, params.deletableArchiveStatus);
        if (notDeletableArchiveId != null) {
            jdbcEntityManager.createArchive(notDeletableArchiveId, params.nonDeletableArchiveStatus);
        }

        var contextToBeArchived = new ArchivingTestJdbcEntityManager.EntityCreationContext();
        var contextNotToBeArchived = new ArchivingTestJdbcEntityManager.EntityCreationContext();

        fillContextWithCommonData(contextToBeArchived);
        contextToBeArchived.orderExternalId = "archive-order-ext-id";
        contextToBeArchived.orderFfStatus = "CREATED";

        fillContextWithCommonData(contextNotToBeArchived);
        contextNotToBeArchived.orderExternalId = "remain-order-ext-id";
        contextNotToBeArchived.orderFfStatus = "CREATED";

        List<TestEntity> creationOrder;
        if (params.overrideExecutionOrder == null) {

            List<TestEntity> defaultCreationOrder =
                    entitiesToCreate.stream().map(re -> re.first).toList();
            if (!defaultCreationOrder.contains(testEntity)) {
                defaultCreationOrder.add(testEntity);
            }
            creationOrder = defaultCreationOrder;
        } else {
            creationOrder = params.overrideExecutionOrder;
        }

        for (TestEntity entityToCreate : creationOrder) {
            Long recordToBeArchivedArchiveId = -1L;
            Long recordNotToBeArchivedArchiveId = notDeletableArchiveId;

            final long recordIdToBeArchived = jdbcEntityManager.createRecord(entityToCreate,
                    contextToBeArchived,
                    ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE, recordToBeArchivedArchiveId);
            contextToBeArchived.ids.put(entityToCreate, recordIdToBeArchived);

            final long recordIdNotToBeArchived = jdbcEntityManager.createRecord(entityToCreate,
                    contextNotToBeArchived,
                    ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE, recordNotToBeArchivedArchiveId);
            contextNotToBeArchived.ids.put(entityToCreate, recordIdNotToBeArchived);

            archivedEntities.stream().filter(ae -> ae.first.equals(entityToCreate)).findFirst().get().second =
                    recordIdToBeArchived;
            notArchivedEntities.stream().filter(ae -> ae.first.equals(entityToCreate)).findFirst().get().second =
                    recordIdNotToBeArchived;


        }

        //tested method
        sweepStep.sweep();


        archivedEntities.addAll(notArchivedEntities);
        check(archivedEntities);
    }

    private void fillContextWithCommonData(ArchivingTestJdbcEntityManager.EntityCreationContext contextToBeArchived) {
        contextToBeArchived.sortingCenterId = sortingCenter.getId();
        contextToBeArchived.userId = user.getId();
        contextToBeArchived.warehouseId = warehouse.getId();
        contextToBeArchived.deliveryServiceId = deliveryService.getId();
        contextToBeArchived.locationId = location.getId();
        contextToBeArchived.measurementId = measurements.getId();
    }

    void check(List<Triple<TestEntity, Long, Boolean>> relatedEntities) {

        for (Triple<TestEntity, Long, Boolean> relatedEntity : relatedEntities) {
            if (NOT_ARCHIVABLE_ENTITIES.contains(relatedEntity.first)) {
                continue;
            }

            Optional<Map<String, Object>> recordToBeArchived = jdbcEntityManager
                                                    .findRawById(relatedEntity.first, relatedEntity.second);
            System.out.println("entity = " + relatedEntity.first);
            assertThat(recordToBeArchived.isPresent()).isEqualTo(!relatedEntity.third);  // third == archived
        }
    }

    @Builder
    @Value
    static class SweepTestParams {
        String testId;

        TestEntity entityToBuildGraphFrom;
        List<Pair<TestEntity, Boolean>> otherEntities;
        @Nullable
        List<TestEntity> overrideExecutionOrder;
        @Nullable
        Boolean useNotDeletableArchive;
        @Nullable
        String nonDeletableArchiveStatus;
        String deletableArchiveStatus;
    }

    private void enableArchiving() {
        configurationService.mergeValue(ConfigurationProperties.DB_ARCHIVING_ENABLED_PROPERTY, true);
    }


    private record RelatedEntity(TestEntity testEntity, boolean nullable, boolean affectsArchiving) {
        RelatedEntity(TestEntity testEntity) {
            this(testEntity, false, true);
        }

        RelatedEntity(TestEntity testEntity, boolean nullable) {
            this(testEntity, nullable, true);
        }
    }

    private void enableArchivingDelete() {
        configurationService.mergeValue(ConfigurationProperties.DB_ARCHIVING_DELETE_ENABLED_PROPERTY, true);
    }

}
