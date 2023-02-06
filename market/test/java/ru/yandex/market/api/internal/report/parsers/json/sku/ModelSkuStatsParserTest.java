package ru.yandex.market.api.internal.report.parsers.json.sku;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.ModelSkuStats;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.parsers.json.ModelSkuStatsParser;
import ru.yandex.market.api.matchers.ModelSkuStatsMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

public class ModelSkuStatsParserTest extends UnitTestBase {
    private final ModelSkuStatsParser parser = new ModelSkuStatsParser();

    @Test
    public void skuStats() {
        ModelSkuStats modelSkuStats = parse("model-sku-stats.json");
        Assert.assertThat(
            modelSkuStats,
            ModelSkuStatsMatcher.modelSkuStats(
                ModelSkuStatsMatcher.beforeFilters(4),
                ModelSkuStatsMatcher.afterFilters(2)
            )
        );
    }

    private ModelSkuStats parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
