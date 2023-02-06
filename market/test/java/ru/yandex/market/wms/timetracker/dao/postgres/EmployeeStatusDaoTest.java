package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
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
import ru.yandex.market.wms.timetracker.model.EmployeeStatusModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.specification.rsql.RecursivePredicateBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeStatusDao.class,
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
class EmployeeStatusDaoTest {

    @Autowired
    private EmployeeStatusDao employeeStatusDao;

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-dao/data.xml",
            connection = "postgresConnection")
    void findEventTimeLessThan() {

        final EmployeeStatusModel first = EmployeeStatusModel.builder()
                .id(1L)
                .warehouseId(1L)
                .userName("SOF-ZHDA")
                .eventTime(Instant.parse("2021-11-16T13:42:45Z"))
                .status(EmployeeStatus.OUT_WAREHOUSE)
                .assigner("HRMS")
                .build();

        final EmployeeStatusModel second = EmployeeStatusModel.builder()
                .id(2L)
                .warehouseId(1L)
                .userName("SOF-OKUB")
                .eventTime(Instant.parse("2021-11-16T13:43:37Z"))
                .status(EmployeeStatus.OUT_WAREHOUSE)
                .assigner("HRMS")
                .build();

        final List<EmployeeStatusModel> result =
                employeeStatusDao.findEventTimeLessThanAndStatusNotEqualAndSubStatusEmpty(
                        Instant.parse("2021-11-16T13:44:51Z"), List.of(EmployeeStatus.NOT_FOUND));

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertThat(result, containsInAnyOrder(first, second))
        );
    }


    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-dao/data.xml",
            connection = "postgresConnection")
    void findEventTimeLessThanAndByStatusAndArea() {

        final EmployeeStatusModel expected = EmployeeStatusModel.builder()
                .id(11L)
                .warehouseId(1L)
                .userName("SOF-LUNCH")
                .eventTime(Instant.parse("2021-12-01T14:00:00Z"))
                .status(EmployeeStatus.INDIRECTACTIVITY)
                .assigner("WMS")
                .area("LUNCH")
                .build();

        final List<EmployeeStatusModel> result =
                employeeStatusDao.findEventTimeLessThanAndByStatusAndArea(
                        Instant.parse("2021-12-01T14:30:00Z"),
                        List.of(EmployeeStatus.INDIRECTACTIVITY),
                        List.of("LUNCH")
                );

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertThat(result, containsInAnyOrder(expected))
        );
    }
}
