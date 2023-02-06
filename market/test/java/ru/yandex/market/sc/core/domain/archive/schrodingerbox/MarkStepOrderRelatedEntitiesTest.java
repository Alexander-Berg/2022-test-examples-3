package ru.yandex.market.sc.core.domain.archive.schrodingerbox;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinish;
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

@Disabled
@EmbeddedDbTest
public class MarkStepOrderRelatedEntitiesTest {
    private static final boolean QUICK_TEST = true;

    private static final List<String> STATUSES_TO_ARCHIVE = List.of(
            ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM.name(),
            ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF.name()
    );

    private static final List<String> ALL_STATUSES = EnumSet.allOf(ScOrderFFStatus.class).stream().map(Enum::name)
            .toList();

    private static final List<String> STATUSES_NOT_TO_ARCHIVE = ALL_STATUSES.stream()
            .filter(it -> !STATUSES_TO_ARCHIVE.contains(it))
            .toList();
    private static final Map<String, Long> DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS = new HashMap<>();

    static {
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("orders", 300L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_scan_log", 100L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_ff_status_history", 150L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("route_finish_order", 300L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_item", 100L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_ticket", 300L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("order_update_history", 150L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("place", 300L);
        DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS.put("sortable", 300L);
    }


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
    ArchivingTestJdbcEntityManager jdbcEntityCreator;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ArchivingSettingsService archivingSettingsService;

    @MockBean
    TransferManagerService transferManagerService;


    private SortingCenter sortingCenter;
    private Route route;
    private RouteFinish routeFinish;
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

        route = testFactory.storedIncomingCourierDropOffRoute(LocalDate.now(), sortingCenter,
                testFactory.storedCourier());
        routeFinish = testFactory.storedEmptyRouteFinish(route, user);


        warehouse = testFactory.storedWarehouse();
        measurements = testFactory.storedMeasurements();
        location = testFactory.storedLocation();

        enableSchrodingerBox();
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private static List<MarkTestParams> testParams() {
        List<MarkTestParams> testCases = new ArrayList<MarkTestParams>();


        List<TestEntity> entitiesWithArchiveSettings = List.of(
                TestEntity.PLACE,
                TestEntity.ORDER_SCAN_LOG,
                TestEntity.ORDER_FF_STATUS_HISTORY,
                TestEntity.ORDER_UPDATE_HISTORY,
                TestEntity.ORDER_ITEM,
                TestEntity.ORDER_TICKET,
                TestEntity.ROUTE_FINISH_ORDER
//                TestEntity.SORTABLE //temporary disabled

        );
        for (TestEntity testEntity : entitiesWithArchiveSettings) {

            // 1. Archived order have status eligible for archiving, not archived order could have any status.
            // No related entities. Only days to archive matters

            for (String orderToArchiveFfStatus : STATUSES_TO_ARCHIVE) {
                for (String orderNotToArchiveFfStatus : ALL_STATUSES) {
                    // check if records archived properly
                    List<Integer> daysToPersistList = List.of(100, 1000, 10000);

                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId("1-1")
                                .entity(testEntity)
                                .orderToArchiveFfStatus(orderToArchiveFfStatus)
                                .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                                .daysToPersistInDB((long) daysToPersistInDatabase)
                                .recordAgeDays((long) daysToPersistInDatabase)
                                .build();

                        testCases.add(params);
                        if (QUICK_TEST) {
                            break;
                        }
                    }

                    // Check that you can't archive whole table

                    // less than ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE Mark Step's Minimum
                    // amount of days to persist
                    int minDays = Math.toIntExact(ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE);
                    daysToPersistList = List.of(-1000, -1, 0, minDays - 1000, minDays - 1);

                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId("1-2")
                                .entity(testEntity)
                                .orderToArchiveFfStatus(orderToArchiveFfStatus)
                                .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                                .daysToPersistInDB((long) daysToPersistInDatabase)
                                .recordAgeDays((long) daysToPersistInDatabase)
                                .expectException(true)
                                .build();

                        testCases.add(params);
                        if (QUICK_TEST) {
                            break;
                        }
                    }


                    // check other archives are not affected
                    MarkTestParams params = MarkTestParams.builder()
                            .testId("1-3")
                            .entity(testEntity)
                            .orderToArchiveFfStatus(orderToArchiveFfStatus)
                            .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                            .recordAgeDays(ArchiveQueryService.REASONABLE_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE)
                            .daysToPersistInDB(ArchiveQueryService.REASONABLE_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE)
                            .recordBelongsToOtherArchive(true)
                            .build();

                    testCases.add(params);


                    // check if record persists for amount of time defined in database


                    // can't get archive query settings from static method right now. Using cached values.
                    Long daysFromArchiveSettings = DEFAULT_DAYS_FROM_ADD_ARCHIVE_SETTINGS
                                                                .get(testEntity.name().toLowerCase());
                    params = MarkTestParams.builder()
                            .testId("1-4")
                            .entity(testEntity)
                            .orderToArchiveFfStatus(orderToArchiveFfStatus)
                            .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                            .executeDefaultMethod(true)
                            .recordAgeDays(daysFromArchiveSettings)
                            .build();

                    testCases.add(params);

                    // checkIfRecordPersistForReasonableAmountOfTimeIfNullPassed

                    params = MarkTestParams.builder()
                            .testId("1-5")
                            .entity(testEntity)
                            .orderToArchiveFfStatus(orderToArchiveFfStatus)
                            .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                            .daysToPersistInDB(null)
                            .recordAgeDays(daysFromArchiveSettings)
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

            // 2. No related entities. Both orders has eligible creation dates for archiving. Only status matters

            for (String orderToArchiveFfStatus : STATUSES_TO_ARCHIVE) {
                for (String orderNotToArchiveFfStatus : STATUSES_NOT_TO_ARCHIVE) {
                    // check if records archived properly
                    List<Integer> daysToPersistList = List.of(100, 1000, 10000);

                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId("2-1")
                                .entity(testEntity)
                                .orderToArchiveFfStatus(orderToArchiveFfStatus) // eligible for archiving by status
                                .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus) // eligible for archiving by
                                // status
                                .daysToPersistInDB((long) daysToPersistInDatabase)
                                .recordAgeDays((long) daysToPersistInDatabase + 100) // eligible for archiving by date
                                .build();

                        testCases.add(params);
                        if (QUICK_TEST) {
                            break;
                        }
                    }

                    // Check that you can't archive whole table

                    // less than ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASEMark Step's Minimum
                    // amount of days to persist
                    daysToPersistList = List.of(-1000, -1, 0, 1, 2, 5, 10, 20);

                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId("2-2")
                                .entity(testEntity)
                                .orderToArchiveFfStatus(orderToArchiveFfStatus)
                                .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
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

            // 3. Both orders has eligible for archiving statuses and creation dates. One related entity exists for
            // order we are not archiving

            for (String orderToArchiveFfStatus : STATUSES_TO_ARCHIVE) {
                for (String orderNotToArchiveFfStatus : STATUSES_NOT_TO_ARCHIVE) {
                    List<Integer> daysToPersistList = List.of(100, 1000, 10000);

                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId("3-1-" + testEntity.name())
                                .orderToArchiveFfStatus(orderToArchiveFfStatus) // eligible for archiving by status
                                .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus) // eligible for archiving by st.
                                .daysToPersistInDB((long) daysToPersistInDatabase)
                                .recordAgeDays((long) daysToPersistInDatabase + 100) // eligible for archiving by date
                                .entity(testEntity)
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

        testCases.sort(Comparator.comparing(MarkTestParams::getEntity)
                .thenComparing(MarkTestParams::getTestId));
        return testCases;
    }


    @Transactional
    @ParameterizedTest
    @MethodSource("testParams")
    void checkIfRecordsArchivedProperly(MarkTestParams params) {
        System.out.println("test id = " + params.testId);

        //test params
        String orderToArchiveFfStatus = params.orderToArchiveFfStatus;
        String orderNotToArchiveFfStatus = params.orderNotToArchiveFfStatus;
        Long recordAgeDays = params.recordAgeDays;
        Long daysToPersistInDatabase = params.daysToPersistInDB;
        boolean executeDefaultMethod = params.executeDefaultMethod;
        boolean expectException = params.expectException;
        boolean otherArchive = params.recordBelongsToOtherArchive;
        Long otherArchiveId = otherArchive ? 987654321L : null;
        String ruleSet = params.ruleSet;

        //test data preparation
        if (ruleSet != null) {
            archivingSettingsService.setRuleSet(ruleSet);
        } else {
            archivingSettingsService.setRuleSet("TOP_DOWN");
        }

        String orderToBeArchivedExternalId = "archive-order-ext-id";
        long orderToBeArchived = jdbcEntityCreator.createOrder(
                sortingCenter, orderToBeArchivedExternalId,
                orderToArchiveFfStatus,
                warehouse, deliveryService,
                location, measurements, recordAgeDays + 1, -1L);
        System.out.println("order id archived = " + orderToBeArchived);
        String orderNotToBeArchivedExternalId = "remain-order-ext-id";
        long orderNotToBeArchived = jdbcEntityCreator.createOrder(sortingCenter, orderNotToBeArchivedExternalId,
                orderNotToArchiveFfStatus,
                warehouse,
                deliveryService,
                location, measurements, recordAgeDays, null);
        System.out.println("order id remain = " + orderNotToBeArchived);

        long recordIdToBeArchived = jdbcEntityCreator.createRecord(params.entity, sortingCenter.getId(),
                orderToBeArchived, orderToBeArchivedExternalId, routeFinish.getId(), null, otherArchiveId,
                recordAgeDays + 1);

        long recordIdNotToBeArchived = jdbcEntityCreator.createRecord(params.entity, sortingCenter.getId(),
                orderNotToBeArchived, orderNotToBeArchivedExternalId, routeFinish.getId(), null, null,
                recordAgeDays);

        assertThat(archiveRepository.findAll()).isEmpty();

        //tested method
        if (expectException) {
            assertThatThrownBy(() -> executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod));
        } else {
            executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod);
        }

        check(params.entity, expectException, otherArchive, otherArchiveId, recordIdToBeArchived,
                recordIdNotToBeArchived);

    }

    void check(TestEntity testEntity, boolean expectException, @Nullable boolean otherArchive,
               @Nullable Long otherArchiveId, long recordIdToBeArchived, long recordIdNotToBeArchived) {

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
        assertThat(recordNotToBeArchived.get().get("archive_id")).isNull();
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
        @Nullable
        TestEntity entity;
        String orderToArchiveFfStatus;
        String orderNotToArchiveFfStatus;
        Long recordAgeDays;
        Long daysToPersistInDB;
        boolean executeDefaultMethod;
        boolean expectException;
        boolean recordBelongsToOtherArchive;
        String ruleSet;

        @Nullable
        TestEntity existingRelatedEntity;
    }

    private void enableSchrodingerBox() {
        configurationService.mergeValue(ConfigurationProperties.DB_ARCHIVING_ENABLED_PROPERTY, true);
    }


}
