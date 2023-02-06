package ru.yandex.market.crm.campaign.services.sending.template;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
public class Groovy2JinjaTranslatorTest {

    private static void testTranslation(String groovy, String jinja) {
        String translated = Groovy2JinjaTranslator.toJinja(groovy);
        assertEquals(jinja, translated);
    }

    @Test
    public void testTransaleTemplateWithNoVars() {
        String template = "Template with no vars";
        testTranslation(template, template);
    }

    @Test
    public void testTemplateWithSimpleVar() {
        testTranslation(
                "Using variable var = $var",
                "Using variable var = {{var}}"
        );
    }

    @Test
    public void testTemplateWithMultipleVars() {
        testTranslation(
                "First variable is $var1 and second variable is $var2",
                "First variable is {{var1}} and second variable is {{var2}}"
        );
    }

    @Test
    public void testVarsInCurlyBrackets() {
        testTranslation(
                "Using variable var = ${curly}.",
                "Using variable var = {{curly}}."
        );
    }

    @Test
    public void testExpressionWithDot() {
        testTranslation(
                "Using variable var = ${coin.title}.",
                "Using variable var = {{coin.title}}."
        );
    }

    @Test
    public void testExpressionWithArray() {
        testTranslation(
                "Using variable var = ${coin[0]}.",
                "Using variable var = {{coin[0]}}."
        );
    }
}
