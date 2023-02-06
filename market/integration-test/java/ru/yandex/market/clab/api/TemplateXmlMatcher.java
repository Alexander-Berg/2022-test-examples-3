package ru.yandex.market.clab.api;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 06.12.2018
 */
public class TemplateXmlMatcher implements ResultMatcher {

    private final String expected;
    private final Map<String, Object> responseValues;

    public TemplateXmlMatcher(String expected, Map<String, Object> responseValues) {
        this.expected = expected;
        this.responseValues = responseValues;
    }

    @Override
    public void match(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String content = response.getContentAsString();
        assertXmlEqual(expected, content);
    }

    private void assertXmlEqual(String expected, String actual) {
        SkipExpectedDifferences skipExpectedDifferences = new SkipExpectedDifferences();

        Diff diff = DiffBuilder.compare(expected).withTest(actual)
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
            .withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default,
                skipExpectedDifferences))
            .ignoreWhitespace()
            .ignoreComments()
            .checkForSimilar()
            .build();

        if (diff.hasDifferences()) {
            AssertionErrors.fail("Body content " + diff.toString());
        }

        responseValues.putAll(skipExpectedDifferences.getValues());
    }
}
