package ru.yandex.market.wms.timetracker.dao;

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
import ru.yandex.market.wms.timetracker.dao.dash.PerfomanceDictDao;
import ru.yandex.market.wms.timetracker.model.PerfomanceDictDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerfomanceDictDao.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
public class PerfomanceDictDaoTest {

    @Autowired
    private PerfomanceDictDao perfomanceDictDao;

    private final PerfomanceDictDto firstDto = PerfomanceDictDto.builder()
            .overallPerShift(4048)
            .operationNumber("3")
            .operationDescription("Паллетная приемка стандартная")
            .operationType("Приемка")
            .countDataType("itrn")
            .unit("палл")
            .timeUnit(0)
            .ye((float) 12.3)
            .price(0)
            .warehouse("SOF")
            .build();

    private final PerfomanceDictDto secondDto = PerfomanceDictDto.builder()
            .overallPerShift(4048)
            .operationNumber("30")
            .operationDescription("КОМПРЕССИЯ")
            .operationType("Компрессия")
            .unit("час")
            .timeUnit(1)
            .ye(368)
            .price((float) 47.27)
            .warehouse("SOF")
            .build();

    @Test
    @Sql(value = "/repository/perfomance-dict-dao/get-dict/before.sql", executionPhase =
            Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    @Sql(value = "/repository/perfomance-dict-dao/truncate.sql", executionPhase =
            Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(dataSource = "dashDataSource"))
    void performanceDict() {
        List<PerfomanceDictDto> actualList = perfomanceDictDao.getPerfomanceDict();
        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(firstDto, secondDto));
    }
}
