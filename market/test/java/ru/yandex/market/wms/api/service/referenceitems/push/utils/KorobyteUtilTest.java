package ru.yandex.market.wms.api.service.referenceitems.push.utils;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.wms.api.service.referenceitems.utils.KorobyteUtil;
import ru.yandex.market.wms.common.spring.BaseTest;

public class KorobyteUtilTest extends BaseTest {
    @Test
    void checkEmpty() {
        assertions.assertThat(
                KorobyteUtil.calc(null, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0, 0.0)
        ).isEqualTo(Optional.empty());

        assertions.assertThat(
                KorobyteUtil.calc(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0.0, 0.0)
        ).isEqualTo(Optional.empty());

        assertions.assertThat(
                KorobyteUtil.calc(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, null, 0.0)
        ).isEqualTo(Optional.empty());

        assertions.assertThat(
                KorobyteUtil.calc(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0, null)
        ).isEqualTo(Optional.empty());
    }

    @Test
    void checkSimpleKorobyte() {
        Optional<Korobyte> korobyte =
                KorobyteUtil.calc(BigDecimal.valueOf(0.32600), BigDecimal.valueOf(0.32600), BigDecimal.valueOf(0.00),
                        18.0, 6.5, 64.5);

        Korobyte expectedKorobyte = new Korobyte.KorobyteBuiler(7, 18, 65, BigDecimal.valueOf(0.33))
                .setWeightNet(BigDecimal.valueOf(0.33))
                .setWeightTare(BigDecimal.valueOf(0.0).setScale(2))
                .build();

        Assert.assertEquals(korobyte.isPresent(), true);
        Assert.assertEquals(korobyte.get(), expectedKorobyte);
    }
}
