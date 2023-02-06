package ru.yandex.market.health.configs.clickphite.graphite;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

class MetricTest {

    @Test
    void escapeNameTest() {

        Assert.assertEquals(
            "iva1-3119-eac-iva-market-prod-_____-_X__-02c-11861_gencfg-c_yandex_net",
            Metric.escapeName(".iva1-3119-eac-iva-market-prod-()\\/.- !*:;-02c-11861.gencfg-c.yandex.net")
        );

        Assert.assertEquals(
            "some-split_name_with__special_symbols_0_99",
            Metric.escapeName("_some-split.name(with)_special_symbols\\!0.99")
        );
    }
}
