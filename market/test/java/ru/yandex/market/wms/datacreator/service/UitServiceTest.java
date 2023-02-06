package ru.yandex.market.wms.datacreator.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class UitServiceTest extends DataCreatorIntegrationTest {

    @Autowired
    private UitService uitService;

    @Test
    @DatabaseSetup(value = "/dao/uit/before.xml", connection = "wmwhse1Connection")
    void getUitByLocAndLotTest() {
        String uit = uitService.getUitByLocAndLot("loc1", "1111");
        assertThat(uit).isEqualTo("3333");
    }
}
