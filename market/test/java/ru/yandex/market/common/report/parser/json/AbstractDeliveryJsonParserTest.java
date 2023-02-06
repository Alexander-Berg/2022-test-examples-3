package ru.yandex.market.common.report.parser.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import ru.yandex.market.common.report.model.AbstractDelivery;
import ru.yandex.market.common.report.model.AbstractDeliveryResult;
import ru.yandex.market.common.report.model.TariffStats;
import ru.yandex.market.common.report.model.tariffFactors.KgtFactor;
import ru.yandex.market.common.report.model.tariffFactors.TariffFactor;
import ru.yandex.market.common.report.model.tariffFactors.TariffType;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AbstractDeliveryJsonParserTest {

    protected <T extends AbstractDelivery<?>> void testTariffStatsParser(AbstractDeliveryJsonParser<T, ?> parser)
            throws IOException {
        T result = parser.parse(AbstractDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_tariffStats.json"));

        AbstractDeliveryResult res = getOnlyElement(result.getResults());
        TariffStats tariffStats = res.getTariffStats();

        assertNotNull(tariffStats);
        assertNotNull(tariffStats.getKgtInfo());
        assertNotNull(tariffStats.getTariffInfo());

        assertNotNull(tariffStats.getKgtInfo().getText());

        List<KgtFactor> kgtFactors = tariffStats.getKgtInfo().getFactors();

        assertEquals(7, kgtFactors.size());

        // проверяем ожидаемые значения (нормальные)
        assertEquals(TariffType.VOLUME, kgtFactors.get(0).getType());
        assertEquals(0, BigDecimal.valueOf(3).compareTo(kgtFactors.get(0).getOrderValue().getValue()));
        assertEquals("м³", kgtFactors.get(0).getOrderValue().getUnit());

        assertEquals(TariffType.MAX_ITEM_DIM, kgtFactors.get(1).getType());

        assertEquals(TariffType.WEIGHT, kgtFactors.get(2).getType());
        assertEquals(0, BigDecimal.valueOf(5.31).compareTo(kgtFactors.get(2).getOrderValue().getValue()));

        assertEquals(TariffType.UNKNOWN, kgtFactors.get(3).getType());
        assertNull(kgtFactors.get(3).getOrderValue());

        // проверяем "кривые" данные
        assertEquals(TariffType.WEIGHT, kgtFactors.get(4).getType());
        assertEquals(0, BigDecimal.valueOf(5.31).compareTo(kgtFactors.get(4).getOrderValue().getValue()));

        assertEquals(TariffType.WEIGHT, kgtFactors.get(5).getType());
        assertNull(kgtFactors.get(5).getOrderValue());

        assertEquals(TariffType.UNKNOWN, kgtFactors.get(6).getType());
        assertNull(kgtFactors.get(6).getOrderValue());


        // -- проверка факторов
        List<TariffFactor> tariffFactors = tariffStats.getTariffInfo().getFactors();
        assertEquals(2, tariffFactors.size());

        assertEquals(TariffType.VOLUME, tariffFactors.get(0).getType());
        assertEquals(TariffType.WEIGHT, tariffFactors.get(1).getType());
    }
}
