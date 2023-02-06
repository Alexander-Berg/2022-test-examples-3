package ru.yandex.market.common.report.parser.json;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.common.report.model.Promo;
import ru.yandex.market.common.report.model.PromoSearchResults;
import ru.yandex.market.common.report.model.SearchResults;

import java.io.FileInputStream;
import java.util.List;

/**
 * Created by antipov93@yndx-team.ru
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:common-market/common-market.xml")
public class PromoMarketReportJsonParserTest extends Assert {

    @Test
    public void testParseJson() throws Exception {
        PromoMarketReportJsonParserFactory factory = new PromoMarketReportJsonParserFactory();
        factory.setParserSettings(new PromoMarketReportJsonParserSettings());
        PromoMarketReportJsonParser parser = factory.newParser();
        parser.parse(PromoMarketReportJsonParserTest.class.getResourceAsStream("/files/promo.json"));
        PromoSearchResults result = parser.getSearchResults();
        Assert.assertEquals(result.getPromos().size(), 5);
    }
}
