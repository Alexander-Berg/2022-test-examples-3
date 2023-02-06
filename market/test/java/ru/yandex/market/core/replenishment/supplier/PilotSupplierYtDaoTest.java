package ru.yandex.market.core.replenishment.supplier;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.core.yt.YtTablesMockUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PilotSupplierYtDaoTest {
    private static final LocalDate LOCAL_DATE = LocalDate.parse("2021-07-10");

    private PilotSupplierYtDao setUpPilotSupplierYtDao(Yt yt) {
        Clock fixedClock = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(fixedClock.instant());

        return new PilotSupplierYtDao(yt, "//tablePath", clock);
    }

    @Test
    void testGetPilotSupplierIds() throws IOException {
        final Yt yt = YtTablesMockUtils.mockYt(
                this.getClass(),
                "PilotSupplierYtDaoTest.yt.pilotSuppliersData.json"
                , "pilotSuppliers"
        );

        List<Long> pilotSupplierIds = setUpPilotSupplierYtDao(yt).getPilotSupplierIds();
        Assertions.assertEquals(2, pilotSupplierIds.size());
        Assertions.assertEquals(2L, pilotSupplierIds.get(0).longValue());
        Assertions.assertEquals(4L, pilotSupplierIds.get(1).longValue());
    }

    @Test
    void testExceptionAccessYtData() {
        List<Long> pilotSupplierIds = setUpPilotSupplierYtDao(YtTablesMockUtils.mockYtThrowErr()).getPilotSupplierIds();

        assertThat(pilotSupplierIds).isEmpty();
    }
}
