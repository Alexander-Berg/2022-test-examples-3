package ru.yandex.market.api.internal.report.parsers.json;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by fettsery on 01.08.18.
 */
@WithContext
public class MainModelFullSpecificationJsonParserTest extends UnitTestBase {

    @Test
    public void testParseMainModelFullDetails() {
        MainModelFullSpecificationJsonParser parser = new MainModelFullSpecificationJsonParser();
        Long2ObjectMap<List<SpecificationGroup>> result =
            parser.parse(ResourceHelpers.getResource("full-model-details.json"));

        assertNotNull(result);
        List<SpecificationGroup> groups = result.get(13546772);
        assertNotNull(groups);

        assertEquals(14, groups.size());

        SpecificationGroup group = groups.get(10);
        assertEquals("Питание", group.getName());

        List<SpecificationGroup.Feature> features = group.getFeatures();
        assertNotNull(features);
        assertEquals(1, features.size());
        assertEquals("Количество ячеек батареи", features.get(0).getName());
        assertEquals("3", features.get(0).getValue());
    }
}
