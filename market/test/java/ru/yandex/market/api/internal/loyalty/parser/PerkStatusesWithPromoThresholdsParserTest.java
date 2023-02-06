package ru.yandex.market.api.internal.loyalty.parser;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.domain.v2.PerkStatusesWithPromoThresholds;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;

/**
 * Created by shiko-mst on 12.05.22.
 */
public class PerkStatusesWithPromoThresholdsParserTest extends UnitTestBase {
    @Test
    public void shouldParseThresholdsAndPerks() {
        PerkStatusesWithPromoThresholds result = new PerkStatusesWithPromoThresholdsParser().parse(ResourceHelpers.getResource("perks-with-thresholds.json"));

        assertEquals("disabled threshold", result.getDisabledPromoThresholds().get(0).getName());
        assertEquals("test perk", result.getPerkTags().get(0).getTag());

        PerkStatus status = result.getPerkStatuses().get(0);
        assertEquals("promoKey", status.getPromoKey());
        assertEquals("mastercard", status.getPaymentSystem());
        assertEquals(new Integer(14), status.getCashbackPercentNominal());
    }
}
