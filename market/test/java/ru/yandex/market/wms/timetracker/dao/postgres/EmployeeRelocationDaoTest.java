package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.market.wms.timetracker.config.DbConfiguration;
import ru.yandex.market.wms.timetracker.config.TtsTestConfig;
import ru.yandex.market.wms.timetracker.model.AreaModel;
import ru.yandex.market.wms.timetracker.model.EmployeeTimexStatusModel;
import ru.yandex.market.wms.timetracker.model.TimexStatus;
import ru.yandex.market.wms.timetracker.response.EmployeeRelocationRequest;
import ru.yandex.market.wms.timetracker.specification.rsql.RecursivePredicateBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeRelocationDao.class,
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
class EmployeeRelocationDaoTest {

    @Autowired
    private EmployeeRelocationDao dao;

    @Test
    @DatabaseSetup(
            value = "/repository/employee-relocation-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-relocation-dao/insert-single-after.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertSingle() {
        final EmployeeRelocationRequest dto = EmployeeRelocationRequest.builder()
                .wmsLogin("test")
                .position("Кладовщик")
                .eventTime(Instant.parse("2021-10-19T16:15:30Z"))
                .isEntry(true)
                .area(AreaModel.builder().id(1L).build())
                .build();
        dao.insert(1, dto);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-relocation-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-relocation-dao/insert-list-after.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertList() {
        final List<EmployeeRelocationRequest> dtos = List.of(EmployeeRelocationRequest.builder()
                        .wmsLogin("test")
                        .position("Кладовщик")
                        .eventTime(Instant.parse("2021-10-19T16:15:30Z"))
                        .isEntry(true)
                        .area(AreaModel.builder().id(1L).build())
                        .build(),
                EmployeeRelocationRequest.builder()
                        .wmsLogin("test")
                        .position("Кладовщик")
                        .eventTime(Instant.parse("2021-10-19T17:15:30Z"))
                        .isEntry(false)
                        .area(AreaModel.builder().id(1L).build())
                        .build()
        );
        dao.insert(1, dtos);
    }

    @Test()
    @DatabaseSetup(
            value = "/repository/employee-relocation-dao/empty.xml",
            connection = "postgresConnection")
    void insertExceptionWhenEventTimeIsEqual() {
        final List<EmployeeRelocationRequest> dtos = List.of(EmployeeRelocationRequest.builder()
                        .wmsLogin("test")
                        .position("Кладовщик")
                        .eventTime(Instant.parse("2021-10-19T16:15:30Z"))
                        .isEntry(true)
                        .area(AreaModel.builder().id(1L).build())
                        .build(),
                EmployeeRelocationRequest.builder()
                        .wmsLogin("test")
                        .position("Кладовщик")
                        .eventTime(Instant.parse("2021-10-19T16:15:30Z"))
                        .isEntry(false)
                        .area(AreaModel.builder().id(1L).build())
                        .build()
        );

        Assertions.assertThrows(DataAccessException.class,
                () -> dao.insert(1, dtos));
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-relocation-dao/find-active-two-active-before.xml",
            connection = "postgresConnection")
    void findActiveEmployeeTwoActive() {
        final List<EmployeeTimexStatusModel> activeEmployee = dao.findActiveEmployee(1L);

        EmployeeTimexStatusModel testEmployee = EmployeeTimexStatusModel.builder()
                .userName("test")
                .position("Кладовщик")
                .status(TimexStatus.INSIDE)
                .build();

        EmployeeTimexStatusModel ivanovEmployee = EmployeeTimexStatusModel.builder()
                .userName("ivanov")
                .position("Старший кладовщик")
                .status(TimexStatus.INSIDE)
                .build();

        assertAll(
                () -> assertEquals(2, activeEmployee.size()),
                () -> assertThat(activeEmployee, containsInAnyOrder(testEmployee, ivanovEmployee))
        );
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-relocation-dao/find-active-one-active-before.xml",
            connection = "postgresConnection")
    void findActiveEmployeeOneActive() {
        final List<EmployeeTimexStatusModel> activeEmployee = dao.findActiveEmployee(1L);

        EmployeeTimexStatusModel testEmployee = EmployeeTimexStatusModel.builder()
                .userName("test")
                .position("Кладовщик")
                .status(TimexStatus.INSIDE)
                .build();

        assertAll(
                () -> assertEquals(1, activeEmployee.size()),
                () -> assertThat(activeEmployee, containsInAnyOrder(testEmployee))
        );
    }
}
