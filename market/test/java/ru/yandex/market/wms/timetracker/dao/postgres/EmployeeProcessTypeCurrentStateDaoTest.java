package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
import ru.yandex.market.wms.timetracker.dao.EmployeeProcessTypeCurrentStateDao;
import ru.yandex.market.wms.timetracker.model.EmployeeProcessTypeModel;
import ru.yandex.market.wms.timetracker.model.enums.AssigmentType;
import ru.yandex.market.wms.timetracker.model.enums.ProcessType;

import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeProcessTypeCurrentStateDao.class
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
class EmployeeProcessTypeCurrentStateDaoTest {

    @Autowired
    private EmployeeProcessTypeCurrentStateDao employeeProcessTypeCurrentStateDao;

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-current-state-dao/empty.xml",
            connection = "postgresConnection")
    void findByUserNameWhenEmpty() {

        final Optional<EmployeeProcessTypeModel> result = employeeProcessTypeCurrentStateDao
                .findByUserName(1L, "test");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-current-state-dao/find-id-by-name.xml",
            connection = "postgresConnection")
    void findByUserNameWhenNotEmpty() {

        EmployeeProcessTypeModel expected = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        final EmployeeProcessTypeModel result = employeeProcessTypeCurrentStateDao
                .findByUserName(1L, "test")
                .orElseThrow(() -> new RuntimeException("result is null"));

        MatcherAssert.assertThat(result, samePropertyValuesAs(expected));
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-current-state-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-process-type-current-state-dao/save-after.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void save() {
        EmployeeProcessTypeModel expected = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        employeeProcessTypeCurrentStateDao.insert(expected);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-current-state-dao/update-before.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-process-type-current-state-dao/save-after.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void update() {
        EmployeeProcessTypeModel expected = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        employeeProcessTypeCurrentStateDao.update(expected);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-current-state-dao/find-all-before.xml",
            connection = "postgresConnection")
    void findAll() {
        EmployeeProcessTypeModel expectedTest = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        EmployeeProcessTypeModel expectedIvanov = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("ivanov")
                .eventTime(Instant.parse("2021-11-01T15:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        final List<EmployeeProcessTypeModel> result =
                employeeProcessTypeCurrentStateDao.findAll(1L);

        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expectedTest, expectedIvanov));
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-current-state-dao/find-all-by-names-before.xml",
            connection = "postgresConnection")
    void findAllByNames() {

        EmployeeProcessTypeModel expectedIvanov = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("ivanov")
                .eventTime(Instant.parse("2021-11-01T15:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        EmployeeProcessTypeModel expectedPetrov = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("petrov")
                .eventTime(Instant.parse("2021-11-01T15:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        final List<EmployeeProcessTypeModel> result =
                employeeProcessTypeCurrentStateDao.findAllByNames(1L, List.of("ivanov", "petrov"));

        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expectedIvanov, expectedPetrov));
    }
}
