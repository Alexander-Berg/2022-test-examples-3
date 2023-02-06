package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * Created by fettsery on 12.10.18.
 */
@WithContext
public class SpecificationGroupJsonParserTest extends UnitTestBase {
    @Test
    public void testRemoveHtmlTags() {
        ContextHolder.get().setGenericParams(
             new GenericParamsBuilder()
                .setRemoveHtmlTags(true)
                .build()
        );
        SpecificationGroupJsonParser parser = new SpecificationGroupJsonParser();

        SpecificationGroup result = parser.parse(ResourceHelpers.getResource("full-specs-with-html.json"));

        Assert.assertEquals("Неадаптированный текст снабжен комментариями и словарем. Для студентов языковых вузов.", result.getFeatures().get(0).getValue());
    }

    @Test
    public void testKeepHtmlTags() {
        SpecificationGroupJsonParser parser = new SpecificationGroupJsonParser();

        SpecificationGroup result = parser.parse(ResourceHelpers.getResource("full-specs-with-html.json"));

        Assert.assertEquals("Неадаптированный текст снабжен комментариями и словарем.<br /><br />Для студентов языковых вузов.", result.getFeatures().get(0).getValue());
    }
}
