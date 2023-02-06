package ru.yandex.market.crm.external.report;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;

public class SkuSpecificationParserTest {

    @Test
    public void testParser() {
        SkuSpecificationParser parser = new SkuSpecificationParser();
        SkuSpecification specification = parser.parse(getClass().getResourceAsStream(
                "sku_specification_response.json"));

        Assertions.assertNotNull(specification);
        List<SkuSpecification.SpecificationGroup> groups = specification.getGroups();
        MatcherAssert.assertThat(groups, hasSize(2));

        Assertions.assertEquals("Общие характеристики", groups.get(0).getName());
        MatcherAssert.assertThat(groups.get(0).getFeatures(), hasSize(0));

        Assertions.assertEquals("Экран", groups.get(1).getName());
        List<SkuSpecification.SpecificationFeature> features = groups.get(1).getFeatures();
        MatcherAssert.assertThat(features, hasSize(2));

        Assertions.assertEquals("Тип экрана", features.get(0).getName());
        Assertions.assertEquals("цветной AMOLED, 16.78 млн цветов, сенсорный", features.get(0).getValue());

        Assertions.assertEquals("Тип сенсорного экрана", features.get(1).getName());
        Assertions.assertEquals("мультитач, емкостный", features.get(1).getValue());
    }
}
