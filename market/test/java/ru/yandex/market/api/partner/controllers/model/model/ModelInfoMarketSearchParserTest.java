package ru.yandex.market.api.partner.controllers.model.model;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.AbstractParserTest;
import ru.yandex.market.common.report.model.Model;
import ru.yandex.market.common.report.model.Prices;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zoom
 */
public class ModelInfoMarketSearchParserTest extends AbstractParserTest {

    @Test
    void shouldParseWell() throws IOException {
        try (InputStream stream = getContentStream("OK.xml")) {
            ModelInfoMarketSearchParser parser = new ModelInfoMarketSearchParser();
            parser.parse(stream);
            List<Model> models = parser.getModelList();

            assertEquals(models.size(), 2);

            Model model = models.get(0);
            assertEquals(11902200, model.getId());
            assertEquals("LotusGrill LotusGrill", model.getName());

            Prices prices = model.getPrices();
            assertEquals(BigDecimal.valueOf(12500L), prices.getMin());
            assertEquals(BigDecimal.valueOf(13500), prices.getAvg());
            assertEquals(BigDecimal.valueOf(18500), prices.getMax());


            model = models.get(1);
            assertEquals(4602802, model.getId());
            assertEquals("ATLANT ХМ 6025-031", model.getName());

            prices = model.getPrices();
            assertEquals(BigDecimal.valueOf(22010), prices.getMin());
            assertEquals(BigDecimal.valueOf(24000), prices.getAvg());
            assertEquals(BigDecimal.valueOf(32816), prices.getMax());
        }
    }
}
