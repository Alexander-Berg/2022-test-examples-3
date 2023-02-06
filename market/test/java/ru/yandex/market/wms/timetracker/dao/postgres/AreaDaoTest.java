package ru.yandex.market.wms.timetracker.dao.postgres;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
import ru.yandex.market.wms.timetracker.model.AreaModel;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AreaDao.class
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
class AreaDaoTest {

    @Autowired
    private AreaDao areaDao;

    @Test
    @DatabaseSetup(
            value = "/repository/area-dao/find-id-by-name.xml",
            connection = "postgresConnection")
    void findIdByName() {
        List<String> names = List.of("АБК-2", "Улица");

        final Map<String, Long> result = areaDao.findIdByName(names);

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),
                () -> Assertions.assertEquals(1L, result.get("АБК-2")),
                () -> Assertions.assertEquals(2L, result.get("Улица"))
        );
    }

    @Test
    @DatabaseSetup(
            value = "/repository/area-dao/empty.xml",
            connection = "postgresConnection")
    @ExpectedDatabase(
            value = "/repository/area-dao/save-after.xml",
            connection = "postgresConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void save() {

        final Map<String, Long> result = areaDao.save(List.of(
                AreaModel.builder()
                        .name("АБК-2")
                        .build(),
                AreaModel.builder()
                        .name("Улица")
                        .build()
        ));

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),
                () -> Assertions.assertTrue(result.containsKey("АБК-2")),
                () -> Assertions.assertTrue(result.containsKey("Улица"))
        );
    }
}
