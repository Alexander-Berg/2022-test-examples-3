package ru.yandex.market.wms.timetracker.dao.postgres;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
import ru.yandex.market.wms.timetracker.model.PerfomanceHistoryDTO;
import ru.yandex.market.wms.timetracker.model.PerformanceByOperationsDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerfomanceHistoryDAO.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
public class PerfomanceHistoryDaoTest {

    @Autowired
    private PerfomanceHistoryDAO perfomanceHistoryDao;

    private final PerfomanceHistoryDTO firstDto = PerfomanceHistoryDTO.builder()
            .warehouseName("SOF")
            .operationGroup("Другое")
            .operator("Никола Теста")
            .company("Staff")
            .experience("Опытный сотрудник, более 3 мес")
            .role("Администратор")
            .qty(new BigDecimal(29))
            .hour(LocalDateTime.parse("2021-09-21 10:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .supervision("дневная смена")
            .overall((float) 0.4775)
            .spentTime(50)
            .operDay(LocalDate.parse("2021-09-21"))
            .effective("Средняя эффективность")
            .dayOverall(44)
            .build();

    private final PerfomanceHistoryDTO secondDto = PerfomanceHistoryDTO.builder()
            .operator("Никола Теста")
            .warehouseName("SOF")
            .operationGroup("Консолидация")
            .operator("Никола Теста")
            .company("Staff")
            .spentTime(40)
            .experience("Опытный сотрудник, более 3 мес")
            .role("Администратор")
            .qty(new BigDecimal(222))
            .hour(LocalDateTime.parse("2021-09-21 11:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .supervision("дневная смена")
            .overall((float) 1.3)
            .operDay(LocalDate.parse("2021-09-21"))
            .effective("Низкая эффективность")
            .dayOverall(83.5)
            .build();

    private final PerformanceByOperationsDto performanceByOperationsFirstDto = PerformanceByOperationsDto.builder()
            .operator("Никола Теста")
            .storageName("SOF")
            .categoriesName("Другое")
            .date(LocalDateTime.parse("2021-09-21 10:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .result(50)
            .overallByOperation(0.4775)
            .overall(0.6)
            .dayOverall(44)
            .threshold("Средняя эффективность")
            .operDay(LocalDate.of(2021, 9, 21))
            .build();

    private final PerformanceByOperationsDto performanceByOperationsSecondDto = PerformanceByOperationsDto.builder()
            .operator("Никола Теста")
            .storageName("SOF")
            .categoriesName("Консолидация")
            .date(LocalDateTime.parse("2021-09-21 11:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .result(500)
            .overallByOperation(1.3)
            .overall(1.8)
            .dayOverall(83.5)
            .threshold("Низкая эффективность")
            .operDay(LocalDate.of(2021, 9, 21))
            .build();

    private final String operator = "Никола Теста";

    @Test
    @Sql(value = "/repository/perfomance-history-dao/find-by-operator/before.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/repository/perfomance-history-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findByWrongOperatorTest() {
        List<PerformanceByOperationsDto> actualList
                = perfomanceHistoryDao.getPerfomanceByOperator(operator + "1");

        assertEquals(0, actualList.size());
    }


    @Test
    @Sql(value = "/repository/perfomance-history-dao/find-by-operator/before.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/repository/perfomance-history-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findByOperatorTest() {
        List<PerformanceByOperationsDto> actualList = perfomanceHistoryDao.getPerfomanceByOperator(operator);

        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(performanceByOperationsFirstDto, performanceByOperationsSecondDto));
    }

    @Test
    @Sql(value = "/repository/perfomance-history-dao/find-by-operator/before.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/repository/perfomance-history-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findByOperatorCaseInsensitiveTest() {
        var actualList = perfomanceHistoryDao.getPerfomanceByOperator("никола");

        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(performanceByOperationsFirstDto, performanceByOperationsSecondDto));
    }

    @Test
    @Sql(value = "/repository/perfomance-history-dao/find-by-operator/before.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/repository/perfomance-history-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findByNullOperatorTest() {
        assertThrows(NullPointerException.class,
                () -> perfomanceHistoryDao.getPerfomanceByOperator(null));
    }

    @Test
    @Sql(value = "/repository/perfomance-history-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void addHistoryTest() {
        var beforeList = perfomanceHistoryDao.getPerfomanceByOperator(operator);
        assertTrue(beforeList.isEmpty());

        List<PerfomanceHistoryDTO> perfomanceHistoryDTOList = Arrays.asList(firstDto, secondDto);
        perfomanceHistoryDao.insert(perfomanceHistoryDTOList);

        var afterList = perfomanceHistoryDao.getPerfomanceByOperator(operator);
        assertEquals(perfomanceHistoryDTOList.size(), afterList.size());
    }

    @Test
    @Sql(value = "/repository/perfomance-history-dao/find-by-operator/find-all-by-names-before.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/repository/perfomance-history-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findAllByNames() {

        List<PerformanceByOperationsDto> actualList = perfomanceHistoryDao.findAllByNames(List.of("Ivanov", "Petrov"));

        Assertions.assertEquals(2, actualList.size());
    }
}
