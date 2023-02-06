package ru.yandex.market.sc.core.domain.archive.schrodingerbox;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

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
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
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
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.TestEntity.ORDER_SCAN_LOG;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.TestEntity.PLACE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.TestEntity.ROUTE_FINISH_ORDER;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.TestEntity.ROUTE_FINISH_PLACE;


@EmbeddedDbTest
public class MarkStepOrderTest {
    private static final boolean QUICK_TEST = true;

    private static final Long DEFAULT_DAYS_TO_PERSIST_FOR_ORDER = 300L; //see add_configuration.sql
    private static final String MAIN_FLOW = "FINISHED_ORDERS";

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
    ArchivingSettingsService archivingSettingsService;

    @Autowired
    TestFactory testFactory;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ArchiveRepository archiveRepository;

    @Autowired
    ScOrderRepository orderRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ArchivingTestJdbcEntityManager jdbcEntityCreator;

    @Autowired
    ConfigurationService configurationService;

    @MockBean
    TransferManagerService transferManagerService;

    @Autowired
    TransactionTemplate transactionTemplate;

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

        enableDbPurifier();
    }


    @SuppressWarnings("checkstyle:MethodLength")
    private static List<MarkTestParams> testParams() {
        List<MarkTestParams> testCases = new ArrayList<>();


        // 1. Archived order have status eligible for archiving, not archived order could have any status.
        // No related entities. Only days to archive matters

        for (String orderToArchiveFfStatus : STATUSES_TO_ARCHIVE) {
            for (String orderNotToArchiveFfStatus : ALL_STATUSES) {
                // check if records archived properly
                List<Integer> daysToPersistList = List.of(100, 1000, 10000);

                for (Integer daysToPersistInDatabase : daysToPersistList) {
                    MarkTestParams params = MarkTestParams.builder()
                            .testId("1-1")
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
                        .orderToArchiveFfStatus(orderToArchiveFfStatus)
                        .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                        .executeDefaultMethod(true)
                        .recordAgeDays(DEFAULT_DAYS_TO_PERSIST_FOR_ORDER)
                        .orderBelongsToOtherArchive(true)
                        .build();

                testCases.add(params);


                // check if record persists for reasonable amount of time if default method invoked

                params = MarkTestParams.builder()
                        .testId("1-4")
                        .orderToArchiveFfStatus(orderToArchiveFfStatus)
                        .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                        .executeDefaultMethod(true)
                        .recordAgeDays(DEFAULT_DAYS_TO_PERSIST_FOR_ORDER)
                        .build();

                testCases.add(params);

                // checkIfRecordPersistForReasonableAmountOfTimeIfNullPassed

                params = MarkTestParams.builder()
                        .testId("1-5")
                        .orderToArchiveFfStatus(orderToArchiveFfStatus)
                        .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus)
                        .daysToPersistInDB(null)
                        .recordAgeDays(DEFAULT_DAYS_TO_PERSIST_FOR_ORDER)
                        .build();

                testCases.add(params);

                if (QUICK_TEST) {
                    break;
                }
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
                            .orderToArchiveFfStatus(orderToArchiveFfStatus) // eligible for archiving by status
                            .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus) // eligible for archiving by status
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


        // 3. Both orders has eligible for archiving statuses and creation dates. One related entity exists for order
        // we are not archiving
        for (var existingRelatedEntity : TestEntity.orderRelated()) {
            for (String orderToArchiveFfStatus : STATUSES_TO_ARCHIVE) {
                for (String orderNotToArchiveFfStatus : STATUSES_TO_ARCHIVE) {
                    // check if records archived properly


                    List<Integer> daysToPersistList = List.of(100, 1000, 10000);

                    for (Integer daysToPersistInDatabase : daysToPersistList) {
                        MarkTestParams params = MarkTestParams.builder()
                                .testId("3-1-" + existingRelatedEntity.name())
                                .ruleSet("BOTTOM_UP")
                                .orderToArchiveFfStatus(orderToArchiveFfStatus) // eligible for archiving by status
                                .orderNotToArchiveFfStatus(orderNotToArchiveFfStatus) // eligible for archiving by st.
                                .daysToPersistInDB((long) daysToPersistInDatabase)
                                .recordAgeDays((long) daysToPersistInDatabase + 100) // eligible for archiving by date
                                .existingRelatedEntity(existingRelatedEntity)
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

        testCases.sort(Comparator.comparing(MarkTestParams::getTestId));

        return testCases;
    }


    @ParameterizedTest
    @MethodSource("testParams")
    @Disabled
    void checkIfRecordsArchivedProperly(MarkTestParams params) {
        //test params
        Long daysToPersistInDatabase = params.daysToPersistInDB;
        boolean executeDefaultMethod = params.executeDefaultMethod;
        boolean expectException = params.expectException;
        String orderToArchiveFfStatus = params.orderToArchiveFfStatus;
        long recordAgeDays = params.recordAgeDays;
        String orderNotToArchiveFfStatus = params.orderNotToArchiveFfStatus;
        boolean otherArchive = params.orderBelongsToOtherArchive;
        Long otherArchiveId = otherArchive ? 987654321L : null;
        String ruleSet = params.ruleSet;

        String remainOrderExternalId = "remain-order-ext-id";
        long orderNotToBeArchivedId = jdbcEntityCreator.createOrder(sortingCenter, remainOrderExternalId,
                orderNotToArchiveFfStatus,
                warehouse,
                deliveryService,
                location, measurements, recordAgeDays, null);

        long orderToBeArchivedId = jdbcEntityCreator.createOrder(sortingCenter, "archive-order-ext-id",
                orderToArchiveFfStatus,
                warehouse, deliveryService,
                location, measurements, recordAgeDays + 1, otherArchiveId);

        transactionTemplate.execute(ts -> {
            System.out.println("test id = " + params.testId);

            //test data preparation
            if (ruleSet != null) {
                archivingSettingsService.setRuleSet(ruleSet);
            } else {
                archivingSettingsService.setRuleSet(MAIN_FLOW);
            }

            if (params.createOrderScanLog) {
                long scanLogNotToBeArchivedId2 = jdbcEntityCreator.createScanLog(sortingCenter, remainOrderExternalId,
                        recordAgeDays + 1,
                        otherArchiveId);
            }
            if (params.existingRelatedEntity != null) {
                if (ORDER_SCAN_LOG.equals(params.existingRelatedEntity)) {
                    jdbcEntityCreator.createScanLog(sortingCenter, remainOrderExternalId, recordAgeDays + 1,
                            otherArchiveId);
                } else if (params.existingRelatedEntity.equals(ROUTE_FINISH_ORDER)) {
                    jdbcEntityCreator.createRecord(params.existingRelatedEntity, sortingCenter.getId(),
                            orderNotToBeArchivedId, remainOrderExternalId, routeFinish.getId(), null, null);
                } else if (params.existingRelatedEntity.equals(ROUTE_FINISH_PLACE)) {
                    long placeId = jdbcEntityCreator.createRecord(PLACE, sortingCenter.getId(),
                            orderNotToBeArchivedId, remainOrderExternalId, routeFinish.getId(), null, null);
                    jdbcEntityCreator.createRecord(params.existingRelatedEntity, sortingCenter.getId(),
                            orderNotToBeArchivedId, remainOrderExternalId, routeFinish.getId(), placeId, null);
                } else {
                    jdbcEntityCreator.createRecord(params.existingRelatedEntity, sortingCenter.getId(),
                            orderNotToBeArchivedId, remainOrderExternalId, null, null, null);
                }
            }
            return null;
        });

        assertThat(archiveRepository.findAll()).isEmpty();

        //tested method
        if (expectException) {
            assertThatThrownBy(() -> executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod));
        } else {
            executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod);
            transactionTemplate.execute(ts -> {
                check(expectException, otherArchive, otherArchiveId,
                        orderToBeArchivedId, orderNotToBeArchivedId);
                return null;
            });
        }
    }


    void check(boolean expectException, boolean otherArchive, @Nullable Long otherArchiveId,
               long orderToBeArchivedId, long orderNotToBeArchivedId) {
        //hacky hacks to refresh cache
        entityManager.flush();
        orderRepository.findAll();

        //result check

        //archive created and is in proper status
        List<Archive> archives = archiveRepository.findAll();
        assertThat(archives.size()).isEqualTo(1);
        Archive archive = archives.get(0);
        if (expectException) {
            assertThat(archive.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_FAILED);
        } else {
            assertThat(archive.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_FINISHED);
        }


        Optional<ScOrder> orderToBeArchived = orderRepository.findById(orderToBeArchivedId);
        assertThat(orderToBeArchived).isPresent();
        var archiveAssert = assertThat(orderToBeArchived.get().getArchiveId());


        if (otherArchive) {
            archiveAssert.isEqualTo(otherArchiveId);
        } else {
            if (expectException) {
                archiveAssert.isNull();
            } else {
                archiveAssert.isNotNull();
            }
        }

        Optional<ScOrder> orderNotToBeArchived = orderRepository.findById(orderNotToBeArchivedId);
        assertThat(orderNotToBeArchived).isPresent();
        assertThat(orderNotToBeArchived.get().getArchiveId()).isNull();
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
        Long recordAgeDays;
        Long daysToPersistInDB;
        String orderToArchiveFfStatus;
        String orderNotToArchiveFfStatus;
        boolean executeDefaultMethod;
        boolean expectException;
        boolean orderBelongsToOtherArchive;
        boolean createOrderScanLog;
        String ruleSet;
        @Nullable
        TestEntity existingRelatedEntity;

    }

    private void enableDbPurifier() {
        configurationService.mergeValue(ConfigurationProperties.DB_ARCHIVING_ENABLED_PROPERTY, true);
    }


}
