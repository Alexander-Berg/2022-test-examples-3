package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.market.wms.timetracker.config.DbConfiguration;
import ru.yandex.market.wms.timetracker.config.TtsTestConfig;
import ru.yandex.market.wms.timetracker.model.EmployeeStatusPlanHistoryModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeStatusPlanHistoryDaoImpl.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@DbUnitConfiguration(
        databaseConnection = {"postgresConnection"})
class EmployeeStatusPlanHistoryDaoImplTest {

    @Autowired
    private EmployeeStatusPlanHistoryDaoImpl employeeStatusPlanHistoryDao;

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-plan-history-dao/before-find-avg.xml",
            connection = "postgresConnection")
    void findAvgByWarehouseIdAndEventTimeBetween() {

        var result =
                employeeStatusPlanHistoryDao.findAvgByWarehouseIdAndEventTimeBetween(
                1L,
                Instant.parse("2021-12-17T12:00:00Z"),
                Instant.parse("2021-12-17T13:00:00Z"));

        var expected =
                EmployeeStatusPlanHistoryModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.CONSOLIDATION)
                        .eventTime(Instant.parse("2021-12-17T12:00:00Z"))
                        .plan(20)
                        .fact(25)
                        .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(1, result.size()),
                () -> Matchers.contains(expected)
        );
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-plan-history-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-status-plan-history-dao/after-insert.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        employeeStatusPlanHistoryDao.insert(List.of(
                EmployeeStatusPlanHistoryModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.CONSOLIDATION)
                        .eventTime(Instant.parse("2021-12-17T12:01:00Z"))
                        .plan(20)
                        .fact(25)
                        .build(),
                EmployeeStatusPlanHistoryModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.CONSOLIDATION)
                        .eventTime(Instant.parse("2021-12-17T12:02:00Z"))
                        .plan(20)
                        .fact(25)
                        .build()));
    }
}
