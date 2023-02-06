package ru.yandex.market.wms.timetracker.dao;

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
import ru.yandex.market.wms.timetracker.dao.dash.PerformancePerHourDao;
import ru.yandex.market.wms.timetracker.model.PerformancePerHourDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerformancePerHourDao.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
public class PerformancePerHourDaoTest {

    @Autowired
    private PerformancePerHourDao performancePerHourDao;

    @Test
    @Sql(value = "/repository/performance-per-hour-dao/before.sql", executionPhase =
            Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    @Sql(value = "/repository/performance-per-hour-dao/truncate.sql", executionPhase =
            Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    void performancePerHourTest() {

        List<PerformancePerHourDto> actualList = performancePerHourDao.getPerformanceByHour();

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, actualList.size()),

                () -> assertThat(actualList, containsInAnyOrder(
                        PerformancePerHourDto.builder()
                                .hour(LocalDateTime.of(2021, 9, 19, 0, 0, 0))
                                .overall(99.56)
                                .username("test")
                                .build(),
                        PerformancePerHourDto.builder()
                                .hour(LocalDateTime.of(2021, 9, 19, 8, 0, 0))
                                .overall(0.54)
                                .username("test")
                                .build()
                ))
        );
    }
}
