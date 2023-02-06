package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.MatcherAssert;
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
import ru.yandex.market.wms.timetracker.dto.EmployeeStatusPlanModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.specification.rsql.RecursivePredicateBuilder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeStatusPlanDao.class,
        RecursivePredicateBuilder.class
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
class EmployeeStatusPlanDaoTest {

    @Autowired
    private EmployeeStatusPlanDao employeeStatusPlanDao;

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-plan-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-status-plan-dao/after-insert.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        employeeStatusPlanDao.insert(List.of(
                EmployeeStatusPlanModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.SHIPPING)
                        .count(10)
                        .lastUpdate(Instant.parse("2021-11-26T12:00:00Z"))
                        .build(),
                EmployeeStatusPlanModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.PLACEMENT)
                        .count(30)
                        .lastUpdate(Instant.parse("2021-11-26T12:00:00Z"))
                        .build()
        ));
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-plan-dao/before-update.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-status-plan-dao/after-update.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() {
        employeeStatusPlanDao.update(List.of(
                EmployeeStatusPlanModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.SHIPPING)
                        .count(30)
                        .lastUpdate(Instant.parse("2021-11-26T12:00:00Z"))
                        .build(),
                EmployeeStatusPlanModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.PLACEMENT)
                        .count(50)
                        .lastUpdate(Instant.parse("2021-11-26T12:00:00Z"))
                        .build()
        ));
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-plan-dao/before-update.xml",
            connection = "postgresConnection")
    void findAll() {

        final EmployeeStatusPlanModel shipping = EmployeeStatusPlanModel.builder()
                .warehouseId(1L)
                .status(EmployeeStatus.SHIPPING)
                .count(10)
                .lastUpdate(Instant.parse("2021-11-26T12:00:00Z"))
                .build();

        final EmployeeStatusPlanModel placement = EmployeeStatusPlanModel.builder()
                .warehouseId(1L)
                .status(EmployeeStatus.PLACEMENT)
                .count(30)
                .lastUpdate(Instant.parse("2021-11-26T12:00:00Z"))
                .build();

        final List<EmployeeStatusPlanModel> result = employeeStatusPlanDao.findAll(1L);

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),
                () -> MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(shipping, placement))
        );
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-plan-dao/before-update.xml",
            connection = "postgresConnection")
    void findAllWhereStatusIn() {

        final EmployeeStatusPlanModel placement = EmployeeStatusPlanModel.builder()
                .warehouseId(1L)
                .status(EmployeeStatus.PLACEMENT)
                .count(30)
                .lastUpdate(Instant.parse("2021-11-26T12:00:00Z"))
                .build();

        final List<EmployeeStatusPlanModel> result = employeeStatusPlanDao.findAllWhereStatusIn(1L,
                List.of(EmployeeStatus.PLACEMENT));

        Assertions.assertAll(
                () -> Assertions.assertEquals(1, result.size()),
                () -> MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(placement))
        );

    }
}
