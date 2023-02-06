package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.offer.CreditInfo;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * Created by fettsery on 18.04.19.
 */
@WithContext
public class CreditInfoJsonParserTest extends UnitTestBase {
    private CreditInfo creditInfo;

    @Before
    public void setUp() {
        byte[] resource = ResourceHelpers.getResource("credit-info.json");
        creditInfo = new CreditInfoJsonParser().parse(resource);
    }

    @Test
    public void checkTerm() {
        Assert.assertEquals(6, creditInfo.getTermRange().getMin());
        Assert.assertEquals(36, creditInfo.getTermRange().getMax());
    }

    @Test
    public void checkBestOptionId() {
        Assert.assertEquals("1", creditInfo.getBestOptionId());
    }

    @Test
    public void checkInitialPayment() {
        Assert.assertEquals("500", creditInfo.getInitialPayment().getValue());
    }

    @Test
    public void checkMonthlyPayment() {
        Assert.assertEquals("300", creditInfo.getMonthlyPayment().getValue());
    }
}
