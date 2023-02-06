package ru.yandex.market.api.internal.report.parsers.json;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 20.03.17.
 */
@WithContext
public class MainModelSpecificationJsonParserTest extends UnitTestBase {

    @Test
    public void testParseMainModelDetails() {
        MainModelSpecificationJsonParser parser = new MainModelSpecificationJsonParser();
        Long2ObjectMap<List<SpecificationGroup>> result =
                parser.parse(ResourceHelpers.getResource("friendly-details.json"));

        assertNotNull(result);
        List<SpecificationGroup> groups = result.get(13546772);
        assertNotNull(groups);

        assertEquals(1, groups.size());
        assertEquals("Общие характеристики", groups.get(0).getName());

        List<SpecificationGroup.Feature> features = groups.get(0).getFeatures();
        assertEquals(12, features.size());
        assertEquals("Процессор: Core i3 / Core i5", features.get(0).getValue());
        assertEquals("Объем жесткого диска: 500 Гб", features.get(3).getValue());
        assertEquals("4G LTE: нет", features.get(8).getValue());
    }

}
