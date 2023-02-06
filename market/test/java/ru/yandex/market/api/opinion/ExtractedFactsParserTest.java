package ru.yandex.market.api.opinion;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.version.ModelFactsVersion;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ExtractedFactsParserTest extends UnitTestBase {

    private ExtractedFactsParser parser = new ExtractedFactsParser(ModelFactsVersion.V1);

    @Test
    public void testParse() {
        final long modelId = 1L;

        final Facts facts = new Facts();
        facts.setContra(Arrays.asList("Аккумулятор", "Дизайн"));
        facts.setPro(Arrays.asList("Производительность", "Цена", "Экран"));

        Long2ObjectMap<Facts> map = (Long2ObjectMap<Facts>) parser.parse(ResourceHelpers.getResource("facts.xml"));
        assertEquals(1, map.size());
        final Facts actualFacts = map.get(modelId);

        assertEquals(facts, actualFacts);
    }
}
