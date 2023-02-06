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
import ru.yandex.market.wms.timetracker.dao.dash.PerformanceByHourDao;
import ru.yandex.market.wms.timetracker.model.PerformanceByHourDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerformanceByHourDao.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
class PerformanceByHourDaoTest {

    @Autowired
    private PerformanceByHourDao performanceByHourDao;

    @Test
    void performanceByHourWhenUserNameIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> performanceByHourDao.performanceByHour(null, LocalDate.EPOCH));
    }

    @Test
    void performanceByHourWhenUserNameIsEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> performanceByHourDao.performanceByHour("", LocalDate.EPOCH));
    }

    @Test
    void performanceByHourWhenPeriodIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> performanceByHourDao.performanceByHour("test", null));
    }

    @Test
    void performanceByHourWhenTableIsEmpty() {
        final List<PerformanceByHourDto> result = performanceByHourDao.performanceByHour("test",
                LocalDate.EPOCH);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    @Sql(value = "/repository/performance-by-hour-dao/before.sql", executionPhase =
            Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    @Sql(value = "/repository/performance-by-hour-dao/truncate.sql", executionPhase =
            Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    void performanceByHour() {

        final List<PerformanceByHourDto> result = performanceByHourDao.performanceByHour("test",
                LocalDate.of(2021, 9, 19));

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),

                () -> assertThat(result, containsInAnyOrder(
                        PerformanceByHourDto.builder()
                                .hour(LocalDateTime.of(2021, 9, 19, 0, 0, 0))
                                .overall(99.56)
                                .build(),
                        PerformanceByHourDto.builder()
                                .hour(LocalDateTime.of(2021, 9, 19, 8, 0, 0))
                                .overall(0.54)
                                .build())));
    }
}
