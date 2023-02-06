package ru.yandex.market.checkout.carter.report;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.carter.report.model.OfferInfoWithPromo;
import ru.yandex.market.checkout.carter.report.parsers.OfferInfoPromoParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
public class OfferInfoParserTest {

    @Test
    void parserTest() throws Exception {
        OfferInfoPromoParser parser = new OfferInfoPromoParser();
        InputStream is = this.getClass().getResourceAsStream("/files/report_offer_info.json");
        List<OfferInfoWithPromo> result = parser.parse(is);

        assertEquals(1, result.size());
        assertEquals(949, result.get(0).getPrice().intValue());
        assertEquals("oIQzUkdCx-ckAqCyh6dQAQ", result.get(0).getOfferId());
        assertEquals(1, result.get(0).getPromo().length());
        assertEquals("promo-code", result.get(0).getPromo().getJSONObject(0).get("promoType"));
        assertEquals("HXlVNC1QxTAuhOOFiishoA", result.get(0).getPromo().getJSONObject(0).get("promoKey"));
        assertEquals("6910", result.get(0).getDiscount().get("oldMin"));
        assertEquals(86, result.get(0).getDiscount().get("percent"));
        assertEquals(false, result.get(0).getDiscount().get("isBestDeal"));
        assertEquals("5961", result.get(0).getDiscount().get("absolute"));
    }
}
