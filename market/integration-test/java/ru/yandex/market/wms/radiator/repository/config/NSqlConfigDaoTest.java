package ru.yandex.market.wms.radiator.repository.config;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;
import ru.yandex.market.wms.radiator.test.IntegrationTestConstants;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class NSqlConfigDaoTest extends IntegrationTestBackend {

    @Autowired
    private NSqlConfigDao dao;
    @Autowired
    private Dispatcher dispatcher;

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/1.xml", connection = "wh1Connection"),
    })
    void when_setStringConfigValue_then_getStringConfigValue() {
        dispatcher.withWarehouseId(
                IntegrationTestConstants.WH_1_ID,
                () -> {
                    assertThat(dao.getStringConfigValue("p0"), is(equalTo("wh1_p0_v0")));
                    assertThat(dao.getStringConfigValue("p1"), is(equalTo("wh1_p1_v0")));

                    dao.setStringConfigValue("p1", "wh1_p1_v1");

                    assertThat(dao.getStringConfigValue("p0"), is(equalTo("wh1_p0_v0")));
                    assertThat(dao.getStringConfigValue("p1"), is(equalTo("wh1_p1_v1")));
                }
        );
    }
}
