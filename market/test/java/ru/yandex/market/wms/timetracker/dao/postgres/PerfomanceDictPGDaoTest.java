package ru.yandex.market.wms.timetracker.dao.postgres;

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
import ru.yandex.market.wms.timetracker.model.PerfomanceDictDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerfomanceDictPGDao.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
public class PerfomanceDictPGDaoTest {

    @Autowired
    private PerfomanceDictPGDao perfomanceDictDao;

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
    @Sql(value = "/repository/perfomance-dict-pg-dao/get-dict/before.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/repository/perfomance-dict-pg-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getDictTest() {
        List<PerfomanceDictDto> actualList = perfomanceDictDao.getDict();
        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(firstDto, secondDto));
    }

    @Test
    @Sql(value = "/repository/perfomance-dict-pg-dao/truncate.sql",
            config = @SqlConfig(dataSource = "postgresDataSource"),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void insertIntoDictTest() {
        var beforeList = perfomanceDictDao.getDict();
        assertTrue(beforeList.isEmpty());

        List<PerfomanceDictDto> perfomanceDictDtos = Arrays.asList(firstDto, secondDto);

        perfomanceDictDao.insert(perfomanceDictDtos);
        List<PerfomanceDictDto> actualList = perfomanceDictDao.getDict();
        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(firstDto, secondDto));
    }

}
