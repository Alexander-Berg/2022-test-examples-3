package ru.yandex.direct.grid.core.util;

import java.math.BigDecimal;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.client.model.ClientNds;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.grid.core.entity.model.client.GdiClientInfo;

import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.TEST_DATE;

@ParametersAreNonnullByDefault
public class ClientTestDataUtils {

    public static GdiClientInfo getTestGdiClientInfo(Long clientId, Long chefUid, int shard) {
        return new GdiClientInfo()
                .withId(clientId)
                .withChiefUserId(chefUid)
                .withShard(shard)
                .withNonResident(true);
    }

    public static ClientNds getTestClientNds(Long clientId) {
        return new ClientNds()
                .withClientId(clientId)
                .withDateFrom(TEST_DATE.minusDays(10))
                .withDateTo(TEST_DATE.plusDays(10))
                .withNds(Percent.fromPercent(BigDecimal.valueOf(18)));
    }
}
