package ru.yandex.market.wms.timetracker.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import ru.yandex.market.wms.timetracker.dao.dash.PerfomanceByOperDayDao;
import ru.yandex.market.wms.timetracker.model.PerfomanceHistoryDTO;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerfomanceByOperDayDao.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
public class PerfomanceByOperDayDaoTest {

    @Autowired
    private PerfomanceByOperDayDao perfomanceByOperDayDao;

    private final PerfomanceHistoryDTO firstDto = PerfomanceHistoryDTO.builder()
            .warehouseName("SOF")
            .operationGroup("Другое")
            .operator("test")
            .company("Staff")
            .experience("Новый сотрудник, менее 2 нед.")
            .role("Кладовщик")
            .qty(new BigDecimal(9))
            .hour(LocalDateTime.parse("2021-09-15 10:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .supervision("дневная смена")
            .overall((float) 0)
            .overallByOperation((float) 0.16)
            .dayOverall((float) 150.64)
            .operDay(LocalDate.parse("2021-09-15"))
            .effective("Не эффективная работа")
            .result(9)
            .build();

    private final PerfomanceHistoryDTO secondDto = PerfomanceHistoryDTO.builder()
            .warehouseName("SOF")
            .operationGroup("Консолидация")
            .operator("test")
            .company("Staff")
            .experience("Новый сотрудник, менее 2 нед.")
            .role("Кладовщик")
            .qty(new BigDecimal(23))
            .hour(LocalDateTime.parse("2021-09-15 09:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .supervision("дневная смена")
            .overall((float) 0.11345483662105718)
            .overallByOperation((float) 0.16)
            .dayOverall((float) 71.3)
            .operDay(LocalDate.parse("2021-09-15"))
            .effective("Не эффективная работа")
            .result(25)
            .build();

    private final PerfomanceHistoryDTO withNullDto = PerfomanceHistoryDTO.builder()
            .warehouseName("SOF")
            .operationGroup("Другое")
            .operator("test-with-null")
            .company("Staff")
            .experience("Новый сотрудник, менее 2 нед.")
            .role("Кладовщик")
            .qty(new BigDecimal(9))
            .hour(LocalDateTime.parse("2021-09-15 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .supervision("дневная смена")
            .overall((float) 0)
            .overallByOperation((float) 0.16)
            .dayOverall((float) 150.64)
            .operDay(LocalDate.parse("2021-09-15"))
            .effective("Не эффективная работа")
            .result(9)
            .build();

    @Test
    @Sql(value = "/repository/performance-by-oper-day-dao/before.sql", executionPhase =
            Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    @Sql(value = "/repository/performance-by-oper-day-dao/truncate.sql", executionPhase =
            Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    void perfomanceByOperDayTest() {
        final List<PerfomanceHistoryDTO> actualList = perfomanceByOperDayDao.getPerfomanceHistoryByOperDay();

        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(firstDto, secondDto));

    }

    @Test
    @Sql(value = "/repository/performance-by-oper-day-dao/before-d1-is-null.sql", executionPhase =
            Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    @Sql(value = "/repository/performance-by-oper-day-dao/truncate.sql", executionPhase =
            Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    void perfomanceByOperDayTestWhenWorkHourIsNull() {
        final List<PerfomanceHistoryDTO> actualList = perfomanceByOperDayDao.getPerfomanceHistoryByOperDay();

        assertEquals(1, actualList.size());
        assertThat(actualList, containsInAnyOrder(withNullDto));

    }
}
