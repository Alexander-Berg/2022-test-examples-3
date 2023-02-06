package ru.yandex.market.api.internal.report.parsers.json;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by apershukov on 20.03.17.
 */
@WithContext
public class FullModelSpecificationJsonParserTest extends BaseTest {
    @Inject
    private ReportParserFactory factory;


    @Test
    public void testParseBlocks() {
        ReportRequestContext context = new ReportRequestContext();
        context.setFields(Collections.emptyList());

        FullModelSpecificationJsonParser parser = factory.getModelSpecificationParser();
        Pair<ModelV2, List<SpecificationGroup>> result = parser.parse(ResourceHelpers.getResource("full-model-details" +
                ".json"));
        assertNotNull(result);

        List<SpecificationGroup> groups = result.getRight();
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
