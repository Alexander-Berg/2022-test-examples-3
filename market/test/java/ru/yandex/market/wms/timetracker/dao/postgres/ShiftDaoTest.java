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
import ru.yandex.market.wms.timetracker.dto.CurrentShiftModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ShiftDao.class
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
public class ShiftDaoTest {

    @Autowired
    private ShiftDao shiftDao;

    private final List<CurrentShiftModel> expected = List.of(
            CurrentShiftModel.builder()
                    .warehouseId(1L)
                    .username("sof-albovski-d")
                    .position("Кладовщик")
                    .shiftName("СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ")
                    .shiftStart(Instant.parse("2021-11-26T05:15:00Z"))
                    .shiftEnd(Instant.parse("2021-11-26T17:15:00Z")).build(),
            CurrentShiftModel.builder()
                    .warehouseId(1L)
                    .username("sof-ekakomiluh")
                    .position("Кладовщик")
                    .shiftName("МАРКЕТ_ОСНОВНОЙ")
                    .shiftStart(Instant.parse("2021-11-26T05:00:00Z"))
                    .shiftEnd(Instant.parse("2021-11-26T14:00:00Z")).build(),
            CurrentShiftModel.builder()
                    .warehouseId(1L)
                    .username("sof-chelnokova")
                    .position("Кладовщик")
                    .shiftName("СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ")
                    .shiftStart(Instant.parse("2021-11-26T05:15:00Z"))
                    .shiftEnd(Instant.parse("2021-11-26T17:15:00Z")).build(),
            CurrentShiftModel.builder()
                    .warehouseId(1L)
                    .username("sof-tatsevolko")
                    .position("Кладовщик")
                    .shiftName("2/2 ПО 11 Ч.")
                    .shiftStart(Instant.parse("2021-11-26T05:00:00Z"))
                    .shiftEnd(Instant.parse("2021-11-26T17:00:00Z")).build()
    );


    @Test
    @DatabaseSetup(value = "/repository/shift-dao/empty.xml")
    @ExpectedDatabase(
            value = "/repository/shift-dao/inserted-stats.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertAllTest() {
        shiftDao.insertAll(expected);
    }

    @Test
    @DatabaseSetup(value = "/repository/shift-dao/inserted-stats.xml")
    void findAndClearComboTest() {
        var actual = shiftDao.findAll(1L);
        assertEquals(4, expected.size());
        assertTrue(actual.containsAll(expected) && expected.containsAll(actual));
        shiftDao.clearAll(1L);
        var afterList = shiftDao.findAll(1L);
        assertEquals(0, afterList.size());
    }

}
