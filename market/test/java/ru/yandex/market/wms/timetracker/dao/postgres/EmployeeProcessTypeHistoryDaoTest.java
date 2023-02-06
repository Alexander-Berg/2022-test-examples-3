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
import ru.yandex.market.wms.timetracker.dao.EmployeeProcessTypeHistoryDao;
import ru.yandex.market.wms.timetracker.model.EmployeeProcessTypeModel;
import ru.yandex.market.wms.timetracker.model.enums.AssigmentType;
import ru.yandex.market.wms.timetracker.model.enums.ProcessType;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeProcessTypeHistoryDao.class
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
class EmployeeProcessTypeHistoryDaoTest {

    @Autowired
    private EmployeeProcessTypeHistoryDao employeeProcessTypeHistoryDao;

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-history-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/employee-process-type-history-dao/save-after.xml",
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

        employeeProcessTypeHistoryDao.save(expected);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/employee-process-type-history-dao/find-at-time.xml",
            connection = "postgresConnection")
    void findAllWhereEventTimeAfterThan() {
        Instant atTime = Instant.parse("2021-11-01T14:00:00Z");

        EmployeeProcessTypeModel expected12 = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .eventTime(Instant.parse("2021-11-01T12:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        EmployeeProcessTypeModel expected15 = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .eventTime(Instant.parse("2021-11-01T15:00:00Z"))
                .processType(ProcessType.CONSOLIDATION)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .putAwayZoneId(null)
                .build();

        final List<EmployeeProcessTypeModel> result =
                employeeProcessTypeHistoryDao.findAllWhereEventTimeAfterThan(1L, atTime);

        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expected12, expected15));
    }
}
