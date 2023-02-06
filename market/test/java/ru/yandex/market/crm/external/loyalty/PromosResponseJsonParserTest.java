package ru.yandex.market.crm.external.loyalty;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author apershukov
 */
public class PromosResponseJsonParserTest {

    @Test
    public void testParse() {
        PromosResponseJsonParser parser = new PromosResponseJsonParser();

        List<Promo> promos = parser.parse(getClass().getResourceAsStream("promo.json"));
        Assertions.assertNotNull(promos);
        Assertions.assertEquals(1, promos.size());

        Promo promo = promos.get(0);

        Assertions.assertEquals(10590, (long) promo.getId());
        Assertions.assertEquals("Мега-монетка", promo.getName());
        Assertions.assertEquals(3174, (long) promo.getBudget());
    }
}
