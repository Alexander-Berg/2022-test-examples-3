package ru.yandex.market.wms.timetracker.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.config.DbConfiguration;
import ru.yandex.market.wms.timetracker.config.TtsTestConfig;
import ru.yandex.market.wms.timetracker.dao.dash.PerformanceByUserDao;
import ru.yandex.market.wms.timetracker.model.PerformanceByOperationsDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerformanceByUserDao.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
class PerformanceByUserDaoTest {

    @Autowired
    private PerformanceByUserDao performanceByUserDao;

    @Test
    void operationPerformanceByUserWhenUserNameIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> performanceByUserDao.operationPerformanceByUser(null));
    }

    @Test
    void operationPerformanceByUserWhenUserNameIsEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> performanceByUserDao.operationPerformanceByUser(""));
    }

    @Test
    void operationPerformanceByUserWhenTableIsEmpty() {
        final List<PerformanceByOperationsDto> result = performanceByUserDao.operationPerformanceByUser("test");
        Assertions.assertEquals(0, result.size());
    }

    @Test
    @Sql(value = "/repository/performance-by-user-dao/before.sql", executionPhase =
            Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    @Sql(value = "/repository/performance-by-user-dao/truncate.sql", executionPhase =
            Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    void operationPerformanceByUser() {
        final List<PerformanceByOperationsDto> result = performanceByUserDao.operationPerformanceByUser("test");

        Assertions.assertEquals(1, result.size());

        final PerformanceByOperationsDto performanceByOperationsDto = result.get(0);

        final PerformanceByOperationsDto expected = PerformanceByOperationsDto.builder()
                .date(LocalDateTime.of(2021, 9, 15, 10, 0))
                .overall(0.16)
                .result(9)
                .threshold("Не эффективная работа")
                .overallByOperation(0)
                .dayOverall(150.64)
                .operDay(LocalDate.parse("2021-09-16"))
                .categoriesName("Другое")
                .storageName("SOF")
                .build();

        assertThat(performanceByOperationsDto, samePropertyValuesAs(expected));
    }
}
