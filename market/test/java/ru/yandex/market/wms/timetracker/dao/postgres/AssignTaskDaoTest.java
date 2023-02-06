package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
import ru.yandex.market.wms.timetracker.dto.AssignTaskDto;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.specification.rsql.RecursivePredicateBuilder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AssignTaskDao.class,
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
class AssignTaskDaoTest {
    @Autowired
    private AssignTaskDao assignTaskDao;

    @Test
    @DatabaseSetup(
            value = "/repository/assign-task-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "/repository/assign-task-dao/after-insert.xml",
            connection = "postgresConnection")
    void insert() {
        final List<AssignTaskDto> forInsert = List.of(
                AssignTaskDto.builder()
                        .warehouseId(1L)
                        .assigner("assigner")
                        .duration(15L)
                        .status(EmployeeStatus.SHIPPING)
                        .userName("test")
                        .eventTime(Instant.parse("2021-11-24T12:00:00Z"))
                        .build(),
                AssignTaskDto.builder()
                        .warehouseId(1L)
                        .assigner("assigner")
                        .duration(15L)
                        .status(EmployeeStatus.SHIPPING)
                        .userName("test-2")
                        .eventTime(Instant.parse("2021-11-24T12:00:00Z"))
                        .build()
        );

        assignTaskDao.insert(forInsert);
    }
}
