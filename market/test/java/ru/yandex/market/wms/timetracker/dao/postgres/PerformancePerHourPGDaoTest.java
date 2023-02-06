package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
import ru.yandex.market.wms.timetracker.model.PerformanceByHourDto;
import ru.yandex.market.wms.timetracker.model.PerformancePerHourDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerformancePerHourPGDao.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
public class PerformancePerHourPGDaoTest {

    @Autowired
    private PerformancePerHourPGDao performancePerHourPGDao;

    private final PerformancePerHourDto performancePerHourFirstDto = PerformancePerHourDto.builder()
            .hour(LocalDateTime.of(2021, 9, 19, 0, 0, 0))
            .username("test")
            .overall(99.56d)
            .build();

    private final PerformancePerHourDto performancePerHourSecondDto = PerformancePerHourDto.builder()
            .hour(LocalDateTime.of(2021, 9, 19, 8, 0, 0))
            .username("test")
            .overall(0.54d)
            .build();

    private final PerformanceByHourDto firstDto = PerformanceByHourDto.builder()
            .hour(LocalDateTime.of(2021, 9, 19, 0, 0, 0))
            .overall(99.56d)
            .build();

    private final PerformanceByHourDto secondDto = PerformanceByHourDto.builder()
            .hour(LocalDateTime.of(2021, 9, 19, 8, 0, 0))
            .overall(0.54d)
            .build();

    @Test
    @Sql(value = "/repository/performance-per-hour-pg-dao/before.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/repository/performance-per-hour-pg-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getPerformancePerHourTest() {
        List<PerformanceByHourDto> actualList = performancePerHourPGDao.getPerformanceByHour("test",
                LocalDate.of(2021, 9, 19));
        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(firstDto, secondDto));
    }

    @Test
    @Sql(value = "/repository/performance-per-hour-pg-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void insertTest() {
        var beforeList = performancePerHourPGDao.getPerformanceByHour("test",
                LocalDate.of(2021, 9, 19));
        assertTrue(beforeList.isEmpty());

        var performancePerHourDtos = Arrays.asList(performancePerHourFirstDto, performancePerHourSecondDto);

        performancePerHourPGDao.insert(performancePerHourDtos);
        List<PerformanceByHourDto> actualList = performancePerHourPGDao.getPerformanceByHour("test",
                LocalDate.of(2021, 9, 19));
        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(firstDto, secondDto));
    }
}
