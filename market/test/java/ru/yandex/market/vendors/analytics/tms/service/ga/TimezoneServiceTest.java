package ru.yandex.market.vendors.analytics.tms.service.ga;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.service.ga.GaProfileService;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.utils.GaTestUtils.profile;
import static ru.yandex.market.vendors.analytics.core.utils.GaTestUtils.profiles;

/**
 * @author ogonek
 */
@DbUnitDataSet(before = "TimezoneServiceTest.before.csv")
public class TimezoneServiceTest extends FunctionalTest {

    @Autowired
    private TimezoneService timezoneService;

    @MockBean
    private GaProfileService gaProfileService;

    @Test
    @DbUnitDataSet(after = "updateGaCounterTimezonesTest.after.csv")
    void updateGaCounterTimezonesTest() {
        long uid = 5;
        when(gaProfileService.loadProfilesSafe(eq(uid)))
                .thenReturn(Optional.of(profiles(
                        profile("77", "UTC"),
                        profile("78", "UTC+1"),
                        profile("79", "UTC+2")
                )));
        timezoneService.updateGaCounterTimezones();
    }

}
