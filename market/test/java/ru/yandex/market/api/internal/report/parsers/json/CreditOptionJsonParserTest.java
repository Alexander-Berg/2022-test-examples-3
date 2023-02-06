package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.CreditOption;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.math.BigDecimal;

/**
 * Created by fettsery on 19.04.19.
 */
@WithContext
public class CreditOptionJsonParserTest extends UnitTestBase {
    private CreditOption creditOption;

    @Before
    public void setUp() {
        byte[] resource = ResourceHelpers.getResource("credit-option.json");
        creditOption = new CreditOptionJsonParser().parse(resource);
    }

    @Test
    public void checkId() {
        Assert.assertEquals("1", creditOption.getId());
    }

    @Test
    public void checkBank() {
        Assert.assertEquals("sberbank", creditOption.getBank());
    }

    @Test
    public void checkTerm() {
        Assert.assertEquals(12, creditOption.getTerm());
    }

    @Test
    public void checkRate() {
        Assert.assertEquals(BigDecimal.valueOf(16), creditOption.getRate());
    }

    @Test
    public void checkInitialPaymentProcent() {
        Assert.assertEquals(BigDecimal.valueOf(20), creditOption.getInitialPaymentPercent());
    }

    @Test
    public void checkMinPrice() {
        Assert.assertEquals("100", creditOption.getRestrictions().getMinPrice().getValue());
    }
}
