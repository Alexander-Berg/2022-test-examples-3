package ru.yandex.market.wms.timetracker.dao.postgres;

import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
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
import ru.yandex.market.wms.timetracker.model.WarehouseModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        WarehouseDao.class
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
class WarehouseDaoTest {

    @Autowired
    private WarehouseDao dao;

    @Test
    @DatabaseSetup(
            value = "/repository/warehouse-dao/empty.xml",
            connection = "postgresConnection")
    void findAllNamesWhenTableEmpty() {
        assertEquals(0, dao.findAll().size());
    }

    @Test
    @DatabaseSetup(
            value = "/repository/warehouse-dao/data.xml",
            connection = "postgresConnection")
    void findAllNames() {

        final List<WarehouseModel> result = dao.findAll();

        WarehouseModel sof = WarehouseModel.builder()
                .id(1L)
                .name("SOF")
                .timeZone("Europe/Moscow")
                .build();

        WarehouseModel tom = WarehouseModel.builder()
                .id(2L)
                .name("TOM")
                .timeZone("Europe/Moscow")
                .build();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertThat(result, containsInAnyOrder(sof, tom))
        );
    }

    @Test
    @DatabaseSetup(
            value = "/repository/warehouse-dao/data.xml",
            connection = "postgresConnection")
    void findByName() {

        final WarehouseModel result = dao.findByName("SOF")
                .orElseThrow(() -> new RuntimeException("warehouse not found"));

        WarehouseModel sof = WarehouseModel.builder()
                .id(1L)
                .name("SOF")
                .timeZone("Europe/Moscow")
                .build();

        assertThat(result, samePropertyValuesAs(sof));
    }
}
