package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;

import ru.yandex.market.api.domain.v2.ModelOffersProcessingOptions;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertTrue;

/**
 * @author Ural Yulmukhametov <a href="mailto:ural@yandex-team.ru"></a>
 * @date 04.07.2019
 */
public class ReportProcessingOptionsParserTest extends UnitTestBase {

    @Test
    public void shouldParse() {
        ReportProcessingOptionsParser parser = new ReportProcessingOptionsParser<>(ModelOffersProcessingOptions::new);
        ModelOffersProcessingOptions options = (ModelOffersProcessingOptions) parser.parse(
                ResourceHelpers.getResource("model-offers-processing-options.json")
        );

        assertTrue(options.getAdult());
        assertTrue(options.getRestrictionAge18());

    }
}
