package ru.yandex.market.wms.autostart.settings.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.util.PutAwayZoneTestData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/sorting_stations.xml", connection = "wmwhseConnection"),
})
class AutostartZoneSettingsRepositoryTest extends AutostartIntegrationTest {


    @Autowired
    protected AutostartZoneSettingsRepository sut;

    @Test
    void findZoneSettings_S01_ZONE() {
        assertThat(
                sut.findZoneSettings(PutAwayZoneTestData.S01_ZONE),
                is(equalTo(PutAwayZoneTestData.s01Settings()))
        );
    }

    @Test
    void findZoneSettings_S02_ZONE__update__find() {
        assertThat(
                sut.findZoneSettings(PutAwayZoneTestData.S02_ZONE),
                is(equalTo(PutAwayZoneTestData.s02Empty()))
        );

        sut.updateZoneSettings(PutAwayZoneTestData.S02_ZONE, PutAwayZoneTestData.s02Updated());

        assertThat(
                sut.findZoneSettings(PutAwayZoneTestData.S02_ZONE),
                is(equalTo(PutAwayZoneTestData.s02Updated()))
        );
    }
}
