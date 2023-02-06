package ru.yandex.market.sc.core.domain.archive.schrodingerbox;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.archive.ArchiveQueryService;
import ru.yandex.market.sc.core.domain.archive.ArchivingSettingsService;
import ru.yandex.market.sc.core.domain.archive.repository.Archive;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveRepository;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveStatus;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.location.repository.Location;
import ru.yandex.market.sc.core.domain.measurements.repository.Measurements;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.transfermanager.TransferManagerService;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.TestEntity;


@EmbeddedDbTest
@Disabled
public class MarkStepGargantuanTest {
    // Тестировать быстро, а не тщательно
    private static final boolean QUICK_TEST = true;

    /**
     * Отдельный флоу для архивации больших таблиц
     */
    private static final String BIG_TABLE_ARCHIVING_FLOW = "BIG_TABLE_ARCHIVING";

    /**
     * Архивация заказов и сущностей связанных с ними - основной флоу
     */
    private static final String MAIN_FLOW = "FINISHED_ORDERS";

    private static final Map<String, Long> DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS = new HashMap<>();
    private static final HashMap<TestEntity, List<RelatedEntity>> EXPECTED_ARCHIVING_BEHAVIOR = new HashMap<>();
    private static final HashMap<TestEntity, List<TestEntity>> OVERRIDE_EXECUTION_ORDER = new HashMap<>();

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

        // tempoary disabled
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

        // Какие настройки по дням хранения ожидаются в базе
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("orders", 300L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_scan_log", 200L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_ff_status_history", 200L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_item", 200L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_ticket", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_update_history", 200L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("place", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("sortable", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("route_finish_order", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("route_finish_place", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("route_finish", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("route", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("route_cell", 250L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("place_history", 200L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("place_partner_code", 200L);


    }

    private static final List<String> STATUSES_TO_ARCHIVE = List.of(
            ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM.name(),
            ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF.name()
    );

    private static final List<String> ALL_STATUSES = EnumSet.allOf(ScOrderFFStatus.class).stream().map(Enum::name)
            .toList();

    private static final List<String> STATUSES_NOT_TO_ARCHIVE = ALL_STATUSES.stream()
            .filter(it -> !STATUSES_TO_ARCHIVE.contains(it))
            .toList();


    @Autowired
    MarkStep markStep;

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

        enableSchrodingerBox();
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private static List<MarkTestParams> testParams() {
        List<MarkTestParams> testCases = new ArrayList<MarkTestParams>();

        // Тест потока Big table archiving
        var defaultBigTableArchivingEntities =
                List.of(new RelatedEntity(TestEntity.ORDERS, false, false));

        createTestCasesForArchivingPeriods(testCases, TestEntity.ORDER_SCAN_LOG,
                defaultBigTableArchivingEntities, BIG_TABLE_ARCHIVING_FLOW);
        createTestCasesForArchivingPeriods(testCases, TestEntity.ORDER_FF_STATUS_HISTORY,
                defaultBigTableArchivingEntities, BIG_TABLE_ARCHIVING_FLOW);
        createTestCasesForArchivingPeriods(testCases, TestEntity.ORDER_UPDATE_HISTORY,
                defaultBigTableArchivingEntities, BIG_TABLE_ARCHIVING_FLOW);

        // Тест основного потока
        for (TestEntity testEntity : EXPECTED_ARCHIVING_BEHAVIOR.keySet()) {

            var entitiesReferenced = EXPECTED_ARCHIVING_BEHAVIOR.get(testEntity);

            // 1 группа тестов
            createTestCasesForArchivingPeriods(testCases, testEntity, entitiesReferenced, MAIN_FLOW);

            // 2 группа тестов проверяет, что сущности архивируются, если все связанные с ними сущности доступны
            // для архивации на основании ff status
            createTestCasesForOrderRelatedEntitiesForEveryFfStatus(testCases, testEntity, entitiesReferenced);

            // 3 группа тестов. Все возможные комбинации состояний сущностей связанных с архивируемой.
            // зависит от EXPECTED_ARCHIVING_BEHAVIOR заданного в начале этого класса
            createAllPossibleRelatedEntityStateTestCases(testCases, testEntity, entitiesReferenced);


        }

        testCases.sort(Comparator.comparing(MarkTestParams::getArchivedEntity)
                .thenComparing(MarkTestParams::getTestId));
        System.out.println("Total test cases: " + testCases.size());
        return testCases;
    }

    /**
     * Добавляет тест кейсы, которые содержат все возможные комбинации состояний
     * сущностей в базе (удалены, заархивированы), и вычисляет их результаты.
     * Возможные состояния описаны в списке entitiesReferenced в этом классе
     *
     * @param testCases          существующией тест кейсы, куда будут добавленые новые
     * @param testEntity         тестируемая сущность
     * @param entitiesReferenced описание взаимотношений побочных сущностей в тест-кейсе к архивируемой
     */
    private static void createAllPossibleRelatedEntityStateTestCases(List<MarkTestParams> testCases,
                                                                     TestEntity testEntity,
                                                                     List<RelatedEntity> entitiesReferenced) {
        // 3 группа тестов
        List<Pair<TestEntity, Boolean>> alwaysCreate =
                entitiesReferenced.stream().filter(Predicate.not(RelatedEntity::affectsArchiving))
                        .map(e -> new Pair<>(e.testEntity, false)).toList();

        List<RelatedEntity> onlyArchivable = entitiesReferenced.stream()
                .filter(RelatedEntity::affectsArchiving)
                .filter(Predicate.not(RelatedEntity::nullable))
                .toList();

        List<RelatedEntity> nullableAndArchivable = entitiesReferenced.stream()
                .filter(RelatedEntity::affectsArchiving)
                .filter(RelatedEntity::nullable)
                .toList();

        for (int elementCount = 0; elementCount <= onlyArchivable.size(); elementCount++) {
            Iterator<int[]> combinationsToArchive =
                    CombinatoricsUtils.combinationsIterator(onlyArchivable.size(), elementCount);
            while (combinationsToArchive.hasNext()) {
                List<Pair<TestEntity, Boolean>> entityCombinationWithOnlyArchivable = new ArrayList<>(alwaysCreate);
                int[] combinationToArchive = combinationsToArchive.next();

                Set<Integer> setToArchive = new HashSet<>();
                for (int pos : combinationToArchive) {
                    setToArchive.add(pos);
                }

                int archivedFromOnlyArchivable = 0;
                for (int j = 0; j < onlyArchivable.size(); j++) {
                    if (setToArchive.contains(j)) {
                        archivedFromOnlyArchivable++;
                    }
                    entityCombinationWithOnlyArchivable.add(Pair.of(onlyArchivable.get(j).testEntity,
                            setToArchive.contains(j)));
                }

                createTestCasesByAddingNullableAndArchivableEntities(testCases, testEntity, onlyArchivable,
                        nullableAndArchivable,
                        entityCombinationWithOnlyArchivable, archivedFromOnlyArchivable);


            }
        }
    }

    private static void createTestCasesByAddingNullableAndArchivableEntities(List<MarkTestParams> testCases,
                                                                             TestEntity testEntity,
                                                                             List<RelatedEntity> onlyArchivable,
                                                                             List<RelatedEntity> nullableAndArchivable,
                                                                             List<Pair<TestEntity, Boolean>> entityCombinationWithOnlyArchivable,
                                                                             int archivedFromOnlyArchivable) {
        /*
          Вычисляем текущую комбинацию из nullable и archivable сущностей
         */
        for (int insertedEntitiesCount = 0; insertedEntitiesCount <= nullableAndArchivable.size(); insertedEntitiesCount++) {
            Iterator<int[]> combinationsToInsertInDb =
                    CombinatoricsUtils.combinationsIterator(nullableAndArchivable.size(),
                            insertedEntitiesCount);

            while (combinationsToInsertInDb.hasNext()) {
                List<Pair<TestEntity, Boolean>> curCreate2 = new ArrayList<>(entityCombinationWithOnlyArchivable);

                int[] combinationToInsert = combinationsToInsertInDb.next();
                List<Integer> listToInsert = new ArrayList<>();

                for (int pos : combinationToInsert) {
                    listToInsert.add(pos);
                }

                createTestCasesByAddingNullableEntities(testCases, testEntity, onlyArchivable, nullableAndArchivable,
                        entityCombinationWithOnlyArchivable, archivedFromOnlyArchivable, listToInsert);


            }
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void createTestCasesByAddingNullableEntities(List<MarkTestParams> testCases, TestEntity testEntity,
                                                                List<RelatedEntity> onlyArchivable,
                                                                List<RelatedEntity> nullableAndArchivable,
                                                                List<Pair<TestEntity, Boolean>> entityCombinationWithOnlyArchivable,
                                                                int archivedFromOnlyArchivable,
                                                                List<Integer> listToInsert) {
        /*
          Добавляем к текущей комбинации сущностей, учавствующих в тест кейсе nullable и archivable сущности
         */
        Set<Integer> setToInsert = new HashSet<>(listToInsert);

        for (int archivedEntitiesCount = 0; archivedEntitiesCount <= listToInsert.size(); archivedEntitiesCount++) {
            Iterator<int[]> nullableCombinationsToArchive =
                    CombinatoricsUtils.combinationsIterator(nullableAndArchivable.size(),
                            archivedEntitiesCount);
            while (nullableCombinationsToArchive.hasNext()) {
                List<Pair<TestEntity, Boolean>> entityCombinationWithNullable =
                        new ArrayList<>(entityCombinationWithOnlyArchivable);
                int[] nullableCombinationPart = nullableCombinationsToArchive.next();

                List<Integer> listToArchive = new ArrayList<>();
                for (int pos : nullableCombinationPart) {
                    listToArchive.add(pos);
                }
                var savedFromArchivableAndNullable = 0;
                var archivedFromArchivableAndNullable = 0;
                for (int j = 0; j < nullableAndArchivable.size(); j++) {

                    if (setToInsert.contains(j)) {
                        boolean archiveThisEntity = savedFromArchivableAndNullable < listToArchive.size()
                                && listToArchive.get(savedFromArchivableAndNullable).equals(j);
                        entityCombinationWithNullable.add(Pair.of(nullableAndArchivable.get(j).testEntity,
                                archiveThisEntity));
                        savedFromArchivableAndNullable++;
                        if (archiveThisEntity) {
                            archivedFromArchivableAndNullable++;
                        }
                    }

                }

                addCombinationTestCase(testCases, testEntity, onlyArchivable, archivedFromOnlyArchivable,
                        entityCombinationWithNullable, savedFromArchivableAndNullable,
                        archivedFromArchivableAndNullable);

            }
        }
    }

    /**
     * Добавляем тест кейс, где используется сгенерированная комбинация сущностей и их состояний
     */
    private static void addCombinationTestCase(List<MarkTestParams> testCases, TestEntity testEntity,
                                               List<RelatedEntity> onlyArchivable, int archivedFromOnlyArchivable,
                                               List<Pair<TestEntity, Boolean>> finalRelatedEntities,
                                               int savedFromArchivableAndNullable,
                                               int archivedFromArchivableAndNullable) {
        var daysToPersistInDatabase = 100;
        final boolean shouldArchiveTestEntity =
                archivedFromArchivableAndNullable == savedFromArchivableAndNullable
                        && archivedFromOnlyArchivable == onlyArchivable.size();
        MarkTestParams params = MarkTestParams.builder()
                .testId(testEntity.name() + ". 3-1 Check " + testEntity.name() + " "
                        + (shouldArchiveTestEntity ? "should" : "should NOT") +
                        " be archived "
                        + "when following entities in database: " + finalRelatedEntities.toString())
                .archivedEntity(testEntity)
                .relatedEntities(finalRelatedEntities)
                .shouldArchiveTestEntity(
                        shouldArchiveTestEntity
                )
                .markOnlyTable(testEntity.name())
                .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                .daysToPersistInDB((long) daysToPersistInDatabase)
                .recordAgeDays((long) daysToPersistInDatabase + 100) // eligible for archiving by date
                .build();

        testCases.add(params);
    }

    /**
     * Запускаем полный флоу (архивируем все сущности) и смотрим будут ли корректно архивироваться
     * сущности, связанные с заказами
     */
    private static void createTestCasesForOrderRelatedEntitiesForEveryFfStatus(List<MarkTestParams> testCases,
                                                                               TestEntity testEntity,
                                                                               List<RelatedEntity> entitiesReferenced
    ) {
        final List<Pair<TestEntity, Boolean>> entitiesForDefaultCases = entitiesReferenced.stream()
                .map(er -> Pair.of(er.testEntity, false))
                .toList();
        final List<TestEntity> entities =
                entitiesReferenced.stream().map(er -> er.testEntity).toList();

        // 2. No related entities. Both orders has eligible creation dates for archiving. Only status matters
        if (entities.contains(TestEntity.ORDERS)) {
            for (String orderToArchiveFfStatus : STATUSES_TO_ARCHIVE) {
                for (String orderNotToArchiveFfStatus : STATUSES_NOT_TO_ARCHIVE) {

                    // Проверяем, что записи архивируются согласно статусу заказа
                    List<Integer> daysToPersistList = List.of(100, 1000, 10000);

                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId(testEntity.name() + ". 2-1 Archive depends on order status")
                                .archivedEntity(testEntity)
                                .orderToArchiveFfStatus(orderToArchiveFfStatus) // eligible for archiving by status
                                .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus) // eligible for archiving by statu
                                .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                                .relatedEntities(entitiesForDefaultCases)
                                .daysToPersistInDB((long) daysToPersistInDatabase)
                                .recordAgeDays((long) daysToPersistInDatabase + 100) // eligible for archiving by date
                                .build();

                        testCases.add(params);
                        if (QUICK_TEST) {
                            break;
                        }
                    }

                    // Проверить, что таблица не архивируеются, когда запускается полный флоу

                    // less than ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASEMark Step's Minimum
                    // amount of days to persist
                    daysToPersistList = List.of(-1000, -1, 0, 1, 2, 5, 10, 20);
                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId(testEntity.name() + ". 2-2 Can't archive whole table")
                                .archivedEntity(testEntity)
                                .relatedEntities(entitiesForDefaultCases)
                                .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                                .daysToPersistInDB((long) daysToPersistInDatabase)
                                .recordAgeDays((long) daysToPersistInDatabase)
                                .expectException(true)
                                .build();


                        testCases.add(params);
                        if (QUICK_TEST) {
                            break;
                        }
                    }

                    if (QUICK_TEST) {
                        break;
                    }
                }

                if (QUICK_TEST) {
                    break;
                }
            }
        }

    }

    private static void createTestCasesForArchivingPeriods(List<MarkTestParams> testCases, TestEntity testEntity,
                                                           List<RelatedEntity> entitiesReferenced,
                                                           String archivingFlow) {
        // 1. Тестируем архивацию для записей с датами по разные стороны от порога архивации

        final List<Pair<TestEntity, Boolean>> entitiesForDefaultCases = entitiesReferenced.stream()
                .map(er -> Pair.of(er.testEntity, true))
                .toList();


        List<Integer> daysToPersistList = List.of(100, 1000, 10000);

        for (Integer daysToPersistInDatabase : daysToPersistList) {
            MarkTestParams params = MarkTestParams.builder()
                    .testId(testEntity.name() + ". 1-1 " + testEntity.name() + " archived if days_to_persist is " +
                            "passed. Flow: " + archivingFlow)
                    .archivedEntity(testEntity)
                    .relatedEntities(entitiesForDefaultCases)
                    .ruleSet(archivingFlow)
                    .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                    .daysToPersistInDB((long) daysToPersistInDatabase)
                    .recordAgeDays((long) daysToPersistInDatabase)
                    .build();

            testCases.add(params);
            if (QUICK_TEST) {
                break;
            }
        }

        // Проверяем, что нельзя заархивировать всю таблицу

        // Меньше чем ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE - минимальное количество
        // дней для сохранения в базе для Mark Step
        int minDays = Math.toIntExact(ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE);
        daysToPersistList = List.of(-1000, -1, 0, minDays - 1000, minDays - 1);

        for (Integer daysToPersistInDatabase : daysToPersistList) {

            MarkTestParams params = MarkTestParams.builder()
                    .testId(testEntity.name() + ". 1-2 Check that you can't archive whole " + testEntity.name() +
                            "table")
                    .archivedEntity(testEntity)
                    .relatedEntities(entitiesForDefaultCases)
                    .ruleSet(archivingFlow)
                    .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                    .daysToPersistInDB((long) daysToPersistInDatabase)
                    .recordAgeDays((long) daysToPersistInDatabase)
                    .expectException(true)
                    .build();

            testCases.add(params);
            if (QUICK_TEST) {
                break;
            }
        }


        // Проверяем, что другие архивы не изменяются
        MarkTestParams params = MarkTestParams.builder()
                .testId(testEntity.name() + ". 1-3 Other archives for  " + testEntity.name() + " are not affected")
                .archivedEntity(testEntity)
                .relatedEntities(entitiesForDefaultCases)
                .ruleSet(archivingFlow)
                .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                .recordAgeDays(ArchiveQueryService.REASONABLE_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE)
                .daysToPersistInDB(ArchiveQueryService.REASONABLE_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE)
                .recordBelongsToOtherArchive(true)
                .build();

        testCases.add(params);


        // Проверяем, что по умолчанию записи архивируеются, колчество дней, указаное в БД
        Long daysFromArchiveSettings = DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS
                .get(testEntity.name().toLowerCase());
        params = MarkTestParams.builder()
                .testId(testEntity.name()
                        + ". 1-4 " + testEntity.name() + " persists for 1000 days if default method executed")
                .archivedEntity(testEntity)
                .relatedEntities(entitiesForDefaultCases)
                .ruleSet(archivingFlow)
                .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                .executeDefaultMethod(true)
                .recordAgeDays(daysFromArchiveSettings)
                .build();

        testCases.add(params);

        // Проверяем, что если передан null записи архивируеются, колчество дней, указаное в БД
        params = MarkTestParams.builder()
                .testId(testEntity.name() + ". 1-5 " + testEntity.name() + " persists for 1000 days if null days is " +
                        "passed")
                .archivedEntity(testEntity)
                .relatedEntities(entitiesForDefaultCases)
                .ruleSet(archivingFlow)
                .overrideExecutionOrder(OVERRIDE_EXECUTION_ORDER.get(testEntity))
                .daysToPersistInDB(null)
                .recordAgeDays(daysFromArchiveSettings)
                .build();

        testCases.add(params);
    }


    @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
    @Transactional
    @ParameterizedTest
    @MethodSource("testParams")
    void checkIfRecordsArchivedProperly(MarkTestParams params) {
        System.out.println("test id = " + params.testId);
        System.out.println("test params = " + params);

        final long other_archive_id = 987654321L;

        //test params
        String orderToArchiveFfStatus = params.orderToArchiveFfStatus;
        String orderNotToArchiveFfStatus = params.orderNotToArchiveFfStatus;
        Long recordAgeDays = params.recordAgeDays;
        Long daysToPersistInDatabase = params.daysToPersistInDB;
        boolean executeDefaultMethod = params.executeDefaultMethod;
        boolean expectException = params.expectException;
        boolean otherArchive = params.recordBelongsToOtherArchive != null && params.recordBelongsToOtherArchive;
        Long otherArchiveId = otherArchive ? other_archive_id : null;
        String ruleSet = params.ruleSet;
        List<Pair<TestEntity, Boolean>> entitiesToCreate = params.relatedEntities;
        TestEntity testEntity = params.archivedEntity;

        //test data preparation
        if (ruleSet != null) {
            archivingSettingsService.setRuleSet(ruleSet);
        } else {
            archivingSettingsService.setRuleSet(MAIN_FLOW);
        }

        var contextToBeArchived = new ArchivingTestJdbcEntityManager.EntityCreationContext();
        var contextNotToBeArchived = new ArchivingTestJdbcEntityManager.EntityCreationContext();

        fillContextWithCommonData(contextToBeArchived);
        contextToBeArchived.orderExternalId = "archive-order-ext-id";
        contextToBeArchived.orderFfStatus = orderToArchiveFfStatus;

        fillContextWithCommonData(contextNotToBeArchived);
        contextNotToBeArchived.orderExternalId = "remain-order-ext-id";
        contextNotToBeArchived.orderFfStatus = orderNotToArchiveFfStatus;

        List<TestEntity> creationOrder;

        if (params.overrideExecutionOrder == null) {
            List<TestEntity> defaultCreationOrder = entitiesToCreate.stream()
                    .map(re -> re.first)
                    .collect(Collectors.toCollection(ArrayList::new));
            defaultCreationOrder.add(testEntity);
            creationOrder = defaultCreationOrder;
        } else {
            creationOrder = params.overrideExecutionOrder;
        }

        Long recordIdToBeArchived = null;
        Long recordIdNotToBeArchived = null;

        for (TestEntity entityToCreate : creationOrder) {
            Pair<TestEntity, Boolean> relatedEntity =
                    entitiesToCreate.stream().filter(entity -> entity.first.equals(entityToCreate))
                            .findFirst().orElse(null);

            final boolean isTestedEntity = entityToCreate.equals(testEntity);
            if (relatedEntity == null && !isTestedEntity) {
                continue;
            }
            Long recordToBeArchivedArchiveId;

            if (isTestedEntity) {
                recordToBeArchivedArchiveId =
                        otherArchive
                                ? other_archive_id
                                : null;
            } else {
                recordToBeArchivedArchiveId = -1L;
            }

            Long recordNotToBeArchivedArchiveId = isTestedEntity
                    ? null
                    : relatedEntity.second ? -1L : null;

            contextToBeArchived.ids.put(entityToCreate, jdbcEntityManager.createRecord(entityToCreate,
                    contextToBeArchived,
                    recordAgeDays + 1, recordToBeArchivedArchiveId));
            contextNotToBeArchived.ids.put(entityToCreate, jdbcEntityManager.createRecord(entityToCreate,
                    contextNotToBeArchived,
                    recordAgeDays, recordNotToBeArchivedArchiveId));
            if (isTestedEntity) {
                recordIdToBeArchived = contextToBeArchived.ids.get(entityToCreate);
                recordIdNotToBeArchived = contextNotToBeArchived.ids.get(entityToCreate);
            }

        }

        assertThat(archiveRepository.findAll()).isEmpty();

        //tested method
        if (expectException) {
            assertThatThrownBy(() -> executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod));
        } else {
            if (params.markOnlyTable != null) {
                markStep.mark(params.markOnlyTable, daysToPersistInDatabase);
            } else {
                executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod);
            }
        }

        check(testEntity, expectException, otherArchive, otherArchiveId, recordIdToBeArchived,
                recordIdNotToBeArchived, params.shouldArchiveTestEntity != null && params.shouldArchiveTestEntity);

    }

    private void fillContextWithCommonData(ArchivingTestJdbcEntityManager.EntityCreationContext contextToBeArchived) {
        contextToBeArchived.sortingCenterId = sortingCenter.getId();
        contextToBeArchived.userId = user.getId();
        contextToBeArchived.warehouseId = warehouse.getId();
        contextToBeArchived.deliveryServiceId = deliveryService.getId();
        contextToBeArchived.locationId = location.getId();
        contextToBeArchived.measurementId = measurements.getId();
    }

    void check(TestEntity testEntity, boolean expectException, boolean otherArchive,
               @Nullable Long otherArchiveId, long recordIdToBeArchived, long recordIdNotToBeArchived,
               boolean archiveAll) {

        //archive created and is in proper status
        List<Archive> archives = archiveRepository.findAll();
        assertThat(archives.size()).isEqualTo(1);
        Archive archive = archives.get(0);
        if (expectException) {
            assertThat(archive.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_FAILED);
        } else {
            assertThat(archive.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_FINISHED);
        }


        Optional<Map<String, Object>> recordToBeArchived = jdbcEntityManager.findRawById(testEntity,
                recordIdToBeArchived);

        assertThat(recordToBeArchived).isPresent();
        var archiveAssert = assertThat(recordToBeArchived.get().get("archive_id"));

        if (otherArchive) {
            archiveAssert.isEqualTo(otherArchiveId);
        } else {
            if (expectException) {
                archiveAssert.isNull();
            } else {
                archiveAssert.isNotNull();
            }
        }
        Optional<Map<String, Object>> recordNotToBeArchived = jdbcEntityManager.findRawById(testEntity,
                recordIdNotToBeArchived);
        assertThat(recordNotToBeArchived).isPresent();
        if (archiveAll) {
            assertThat(recordNotToBeArchived.get().get("archive_id")).isNotNull();
        } else {
            assertThat(recordNotToBeArchived.get().get("archive_id")).isNull();
        }
    }

    private void executeTestedMethod(Long daysToPersistInDatabase, boolean executeDefaultMethod) {
        if (executeDefaultMethod) {
            markStep.mark();
        } else {
            markStep.mark(daysToPersistInDatabase);
        }
    }

    @Builder
    @Value
    static class MarkTestParams {
        String testId;

        TestEntity archivedEntity;
        String orderToArchiveFfStatus;
        String orderNotToArchiveFfStatus;
        Long recordAgeDays;
        Long daysToPersistInDB;
        boolean executeDefaultMethod;
        boolean expectException;
        @Nullable
        Boolean recordBelongsToOtherArchive;
        @Nullable
        String ruleSet;
        Boolean shouldArchiveTestEntity;
        List<Pair<TestEntity, Boolean>> relatedEntities;
        String markOnlyTable;
        @Nullable
        List<TestEntity> overrideExecutionOrder;
    }

    private void enableSchrodingerBox() {
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

}
