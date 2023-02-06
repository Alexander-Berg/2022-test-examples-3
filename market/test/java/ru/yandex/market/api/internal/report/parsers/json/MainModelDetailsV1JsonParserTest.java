package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.model.Block;
import ru.yandex.market.api.model.ModelDetailsV1;
import ru.yandex.market.api.model.Param;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 16.03.17.
 */
@WithContext
public class MainModelDetailsV1JsonParserTest extends UnitTestBase{

    @Test
    public void testParseDetails() {
        MainModelDetailsV1JsonParser parser = new MainModelDetailsV1JsonParser();
        ModelDetailsV1 details = parser.parse(ResourceHelpers.getResource("friendly-details.json"));

        assertNotNull(details);

        assertEquals(1, details.getBlocks().size());

        Block block = details.getBlocks().get(0);
        assertNull(block.getName());

        List<Param> params = block.getParams();
        assertEquals(11, params.size());

        assertEquals("Процессор", params.get(0).getName());
        assertEquals("Процессор: Core i3 / Core i5", params.get(0).getValue());

        assertEquals("Объем жесткого диска", params.get(3).getName());
        assertEquals("Объем жесткого диска: 500 Гб", params.get(3).getValue());

        assertEquals("4G LTE", params.get(8).getName());
        assertEquals("4G LTE: нет", params.get(8).getValue());
    }
}
