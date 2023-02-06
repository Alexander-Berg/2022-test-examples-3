package ru.yandex.market.wms.timetracker.dao.postgres;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
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
import ru.yandex.market.wms.timetracker.dto.CountByStatusDto;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.specification.rsql.RecursivePredicateBuilder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeStatusWithPerformanceDao.class,
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
class EmployeeStatusWithPerformanceDaoTest {

    @Autowired
    private EmployeeStatusWithPerformanceDao employeeStatusWithPerformanceDao;

    @Test
    @DatabaseSetup(
            value = "/repository/employee-status-with-performance-dao/count-by-status.xml",
            connection = "postgresConnection")
    public void countGroupByStatus() {

        var result = employeeStatusWithPerformanceDao.countGroupByStatus(1L);

        var employeeStatusPlacement = CountByStatusDto.builder()
                .status(EmployeeStatus.PLACEMENT)
                .subStatus("")
                .count(1L)
                .build();

        var employeeStatusInventorization = CountByStatusDto.builder()
                .status(EmployeeStatus.INVENTORIZATION)
                .subStatus("")
                .count(1L)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),
                () -> MatcherAssert.assertThat(result,
                        Matchers.containsInAnyOrder(employeeStatusPlacement, employeeStatusInventorization))
        );
    }
}
