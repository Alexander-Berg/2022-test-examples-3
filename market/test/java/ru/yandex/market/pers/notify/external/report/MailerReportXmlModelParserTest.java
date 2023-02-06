package ru.yandex.market.pers.notify.external.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.common.report.parser.xml.GeneralMarketReportXmlParserFactory;
import ru.yandex.market.pers.notify.entity.OfferModel;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         07.03.17
 */
public class MailerReportXmlModelParserTest extends MarketMailerMockedDbTest {
    @Autowired
    @Qualifier("marketReportModelParserFactory")
    private GeneralMarketReportXmlParserFactory<MailerReportXmlModelParser> modelParserFactory;

    @Test
    public void getOfferModel() throws Exception {
        MailerReportXmlModelParser parser = modelParserFactory.newParser();
        parser.parse(this.getClass().getClassLoader().getResourceAsStream("report/model.xml"));
        OfferModel offerModel = parser.getOfferModel();
        assertEquals("Лампа Camelion E27 30Вт 6400K", offerModel.getModelName());
        assertEquals(1717633985L, (long)offerModel.getModelId());
        assertEquals("Лампочки", offerModel.getCategory());
        assertEquals(90713L, (long)offerModel.getCategoryId());
        assertEquals("https://8.cs-ellpic.yandex.net/market_9RKZhpwiop20zYDDAi-olA_300x400_1.jpg", offerModel.getPictureUrl());
        assertEquals("RUR", offerModel.getCurrency());
        assertEquals(302d, offerModel.getFromPrice(), 0.001);
    }

    @Test
    public void getOfferModelNoPictures() throws Exception {
        MailerReportXmlModelParser parser = modelParserFactory.newParser();
        parser.parse(this.getClass().getClassLoader().getResourceAsStream("report/model_no_pictures.xml"));
        OfferModel offerModel = parser.getOfferModel();
        assertNull(offerModel.getPictureUrl());
    }
}
