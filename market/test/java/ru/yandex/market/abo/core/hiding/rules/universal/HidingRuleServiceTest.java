package ru.yandex.market.abo.core.hiding.rules.universal;

import java.util.Arrays;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.hiding.rules.stopword.model.OfferTag;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author artemmz
 * @date 17/04/19.
 */
class HidingRuleServiceTest extends DeletableEntityServiceTest<HidingRule, Long> {
    private static final long USER_ID = 111L;

    @Autowired
    private HidingRuleService hidingRuleService;

    @Override
    protected DeletableEntityService<HidingRule, Long> service() {
        return hidingRuleService;
    }

    @Override
    protected Long extractId(HidingRule entity) {
        return entity.getId();
    }

    @Override
    protected HidingRule newEntity() {
        HidingRule rule = new HidingRule();
        rule.setShopId(774L);
        rule.setCountry((long) Regions.RUSSIA);
        rule.setRgb(EnumSet.of(Color.RED, Color.GREEN));
        rule.setStopWord("foobar");
        rule.setStopWordTags(Arrays.asList(OfferTag.description, OfferTag.sales_notes));
        rule.setMarketCategoryWhitelistCsv("1, 2, 3");
        rule.setMerchantCategoryBlacklistCsv("234, 4235, 54, buzz");
        rule.setVendorId(123L);
        rule.setDeleted(false);
        rule.setComment("blah blah blah");
        return rule;
    }

    @Override
    protected HidingRule example() {
        return new HidingRule();
    }

    @Test
    @Override
    public void alreadyExists() {
        HidingRule rule = newEntity();
        service().addIfNotExistsOrDeleted(rule, USER_ID);
        HidingRule duplicate = newEntity();
        duplicate.setComment("some other comment");
        duplicate.setRgb(EnumSet.of(Color.BLUE));
        assertThrows(IllegalArgumentException.class, () ->
                service().addIfNotExistsOrDeleted(duplicate, RND.nextLong()));

        HidingRule other = newEntity();
        other.setStopWord("some other stop word");
        service().addIfNotExistsOrDeleted(other, RND.nextLong());
        assertNotEquals(rule.getId(), other.getId());
    }
}