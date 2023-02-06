package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;
import ru.yandex.market.api.domain.v1.DetailsField;
import ru.yandex.market.api.model.Block;
import ru.yandex.market.api.model.ModelDetailsV1;
import ru.yandex.market.api.model.ModelFilter;
import ru.yandex.market.api.model.Param;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 * Created by apershukov on 16.03.17.
 */
public class FullModelDetailsV1JsonParserTest {

    @Test
    public void testParseModelDetails() {
        FullModelDetailsV1JsonParser parser = new FullModelDetailsV1JsonParser(Collections.emptyList());
        ModelDetailsV1 details = parser.parse(ResourceHelpers.getResource("full-model-details.json"));

        assertNotNull(details);

        List<Block> blocks = details.getBlocks();
        assertEquals(14, blocks.size());

        Block block = blocks.get(7);
        assertEquals("Карты памяти", block.getName());

        List<Param> params = block.getParams();
        assertEquals(4, params.size());

        assertEquals("Устройство для чтения флэш-карт", params.get(0).getName());
        assertEquals("Устройство для чтения флэш-карт: есть", params.get(0).getValue());

        assertEquals("Поддержка SD", params.get(1).getName());
        assertEquals("Поддержка SD: есть", params.get(1).getValue());

        assertEquals("Поддержка SDHC", params.get(2).getName());
        assertEquals("Поддержка SDHC: нет", params.get(2).getValue());

        assertEquals("Поддержка SDXC", params.get(3).getName());
        assertEquals("Поддержка SDXC: есть", params.get(3).getValue());

        details.getBlocks().stream()
                .flatMap(x -> x.getParams().stream())
                .forEach(param -> assertNull(param.getFilters()));
    }

    @Test
    public void testParseDetailsWithFilters() {
        FullModelDetailsV1JsonParser parser = new FullModelDetailsV1JsonParser(
                Collections.singletonList(DetailsField.FILTERS));
        ModelDetailsV1 details = parser.parse(ResourceHelpers.getResource("full-model-details.json"));

        Block block = details.getBlocks().get(3);
        assertEquals("Экран", block.getName());

        Param param = block.getParams().get(0);
        assertEquals("Размер экрана", param.getName());

        List<ModelFilter> filters = param.getFilters();
        assertNotNull(filters);
        assertEquals(1, filters.size());

        assertEquals("5085113", filters.get(0).getId());
        assertEquals("Размер экрана", filters.get(0).getName());
    }
}
