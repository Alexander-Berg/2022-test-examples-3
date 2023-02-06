package ru.yandex.market.delivery.transport_manager.controller.health;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.EntityFactory;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.health.product.TransportationHealthChecker;

class TransportationHealthControllerTest extends AbstractContextualTest {

    private static final String OK = "0;OK";

    @Autowired
    TransportationHealthChecker transportationHealthChecker;

    @Autowired
    EntityFactory entityFactory;

    @Test
    @DatabaseSetup("/repository/health/transportation/all.xml")
    void testNextWeekOK() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 14, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNextWeekTransportationExists()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/almost_all.xml")
    void testNextWeekNotOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 14, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNextWeekTransportationExists()).isEqualTo(
            "2;Some schedules exist, but no transportation is planned for dates: " +
                "[2020-10-17, 2020-10-18]");
    }

    @Test
    void testNextWeekNoneOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 14, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNextWeekTransportationExists()).isEqualTo(
            "2;No schedules available!");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_transportations.xml")
    void hasNoFailedTransportationsNotOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 14, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoErrorTransportationsLastWeek())
            .isEqualTo(
                "2;Some transportations for last 7 days are in error state. [{date=2020-10-12, status=ERROR, " +
                    "amount=1}]"
            );
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_transportations.xml")
    void hasNoFailedTransportationsOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 24, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoErrorTransportationsLastWeek()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_transportations.xml")
    void hasNoNonTerminalTransportationsForTomorrowOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 11, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_transportations.xml")
    void hasNoNonTerminalTransportationsForTomorrowButOneFailedNotOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutGoodStatusAfterCutoff())
            .matches(
                "2;100[.,]00% of transportations with types=\\[ORDERS_OPERATION, ORDERS_RETURN] for 2020-10-12 are " +
                    "not in good status after cutoff. \\[\\{date=2020-10-12, " +
                    "status=ERROR, amount=1}]");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_and_valid_transportation.xml")
    void hasNoNonTerminalTransportationsForTomorrowNotOK() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 11, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .matches(
                "2;66[.,]67% of transportations with types=\\[ORDERS_OPERATION, ORDERS_RETURN] for 2020-10-12 are " +
                    "not in allowed status after cutoff. \\[\\{date=2020-10-12, " +
                    "status=SCHEDULED, amount=2}]");
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/health/transportation/xdoc.xml"
    })
    void checkAllowedStatusXDoc() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 11, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .matches(
                "2;66[.,]67% of transportations with types=\\[LINEHAUL, XDOC_TRANSPORT] for 2020-10-15 are not in " +
                    "allowed status after cutoff. \\[\\{date=2020-10-15, status=DRAFT, amount=2}]");
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/health/transportation/xdoc_and_opko.xml",
    })
    void checkAllowedStatusBothTypes() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 11, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .matches(
                "2;100[,.]00% of transportations with types=\\[ORDERS_OPERATION, ORDERS_RETURN] " +
                    "for 2020-10-12 are not in allowed status after cutoff. \\[\\{date=2020-10-12, status=SCHEDULED, " +
                    "amount=1}], " +
                    "66[.,]67% of transportations with types=\\[LINEHAUL, XDOC_TRANSPORT] for 2020-10-15 are not in " +
                    "allowed " +
                    "status " +
                    "after cutoff. \\[\\{date=2020-10-15, status=DRAFT, amount=2}]");
    }

    @Test
    void checkAllowedStatusWarnLimit() {
        clock.setFixed(
            LocalDateTime.of(2021, 2, 19, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );

        entityFactory.createDummyTransportations(
            1,
            TransportationType.ORDERS_OPERATION,
            TransportationStatus.NEW,
            true
        );

        entityFactory.createDummyTransportations(
            99,
            TransportationType.ORDERS_OPERATION,
            TransportationStatus.COULD_NOT_BE_MATCHED,
            true
        );

        clock.setFixed(
            LocalDateTime.of(2021, 2, 18, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );

        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .matches(
                "1;1[.,]00% of transportations with types=\\[ORDERS_OPERATION, ORDERS_RETURN] for 2021-02-19 are " +
                    "not in allowed status after cutoff. \\[\\{date=2021-02-19, status=NEW, amount=1}]");
    }

    @Test
    void checkAllowedStatusCritLimit() {
        clock.setFixed(
            LocalDateTime.of(2021, 2, 19, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );

        entityFactory.createDummyTransportations(
            6,
            TransportationType.ORDERS_OPERATION,
            TransportationStatus.NEW,
            true
        );

        entityFactory.createDummyTransportations(
            94,
            TransportationType.ORDERS_OPERATION,
            TransportationStatus.COULD_NOT_BE_MATCHED,
            true
        );

        clock.setFixed(
            LocalDateTime.of(2021, 2, 18, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );

        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .matches(
                "2;6[.,]00% of transportations with types=\\[ORDERS_OPERATION, ORDERS_RETURN] for 2021-02-19 are " +
                    "not in allowed status after cutoff. \\[\\{date=2021-02-19, " +
                    "status=NEW, amount=6}]");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_and_valid_transportation.xml")
    void ensureNoScheduledTransportationAfterLaunchTime() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 11, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoScheduledTransportationAfterLaunchTime())
            .isEqualTo("2;1 transportations were in SCHEDULED status with planned launch time in the past");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_deleted_transportation.xml")
    void hasOnlyDeletedNonTerminalTransportationsForTomorrowOK() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 11, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_and_valid_transportation.xml")
    void hasNoNonTerminalTransportationsForTodayBeforeNewCutoffNotOK() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .matches(
                "2;66[.,]67% of transportations with types=\\[ORDERS_OPERATION, ORDERS_RETURN] for 2020-10-12 are " +
                    "not in allowed status after cutoff. \\[\\{date=2020-10-12, " +
                    "status=SCHEDULED, amount=2}]");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/failed_and_valid_transportation.xml")
    void hasNoNonTerminalTransportationsForTodayFailedButExpiredOK() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoTransportationsWithoutAllowedStatusAfterCutoff())
            .isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/orders_operation_ok.xml")
    void checkOrdersOperationsOk() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoOrdersOperationInErrorStatus())
                .isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/order_operation_error.xml")
    void checkOrdersOperationsError() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 12, 21, 20, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoOrdersOperationInErrorStatus())
                .startsWith("2;");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/order_operation_error_several_days.xml")
    void checkOrdersOperationsErrorWithSeveralDates() {
        clock.setFixed(
                LocalDateTime.of(2020, 10, 12, 21, 20, 0).toInstant(ZoneOffset.UTC),
                ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoOrdersOperationInErrorStatus())
                .startsWith("2;")
                .contains("2020-10-11");
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/return_dropoff_ok.xml")
    void checkReturnDropoffOk() {
        clock.setFixed(
                LocalDateTime.of(2020, 10, 12, 21, 20, 0).toInstant(ZoneOffset.UTC),
                ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoReturnDropoffInErrorStatus())
                .isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/transportation/return_dropoff_error.xml")
    void checkReturnDropOffError() {
        clock.setFixed(
                LocalDateTime.of(2020, 10, 12, 21, 20, 0).toInstant(ZoneOffset.UTC),
                ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
        softly.assertThat(transportationHealthChecker.ensureNoReturnDropoffInErrorStatus())
                .startsWith("2;");
    }
}
