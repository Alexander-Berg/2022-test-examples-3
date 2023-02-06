package ru.yandex.market.mbo.cms.core.models;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.property.Property;

/**
 * Тест проверяет корректность парсинга плейсхолдеров в шаблонах.
 *
 * @author s-ermakov
 */
public class FieldTest {

    @Test
    public void testSimplePlaceholderParsing() {
        FieldType actual = PlaceholderUtils.parsePlaceholder("__TITLE__");
        FieldType expected = PlaceholderBuilder.newBuilder("TITLE")
                .setAllowedMaxNumberOfWidgets(1)
                .create();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNotPlaceholderFormatParsing() {
        String[] illegalPlaceholdes = new String[]{"TITLE_WITHOUT_DELIMERS", "__TITLE_", "TITLE__", "__TITLE(__",
                "__TITLE)__", "__TITLE()___"};
        for (String illegalPlaceholder : illegalPlaceholdes) {
            try {
                PlaceholderUtils.parsePlaceholder(illegalPlaceholder);
                Assert.fail("Expected IllegalArgumentException to be thrown from '" + illegalPlaceholder + "'");
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
    }

    @Test
    public void testSimplePlaceholderParsingWithIllegalSymbols() {
        String[] illegalSymbols = new String[]{"a", "@", "[", "]", "$", "/", "\\", " "};
        for (String illegalSymbol : illegalSymbols) {
            String placeholderAsString = "__TITLE" + illegalSymbol + "__";
            try {
                PlaceholderUtils.parsePlaceholder(placeholderAsString);
                Assert.fail("Expected IllegalArgumentException to be thrown from '" + placeholderAsString + "'");
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
    }

    @Test
    public void testSimplePlaceholderParsingWithQuantifier() {
        FieldType actual = PlaceholderUtils.parsePlaceholder("__TITLE{1}__");
        FieldType expected = PlaceholderBuilder.newBuilder("TITLE")
                .setAllowedMinNumberOfWidgets(1)
                .setAllowedMaxNumberOfWidgets(1)
                .create();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPlaceholderWithTemplateParsing() {
        FieldType actual = PlaceholderUtils.parsePlaceholder("__TITLE(HEADER ,TEXT, SUBTEXT)__");
        FieldType expected = PlaceholderBuilder.newBuilder("TITLE")
                .addType("HEADER")
                .addType("TEXT")
                .addType("SUBTEXT")
                .create();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPlaceholderWithDuplicatedTemplates() {
        FieldType p = PlaceholderUtils.parsePlaceholder("__TITLE(HEADER,TEXT,TEXT)__");

        List<String> allowedTypes = p.getProperties().get(Property.ALLOWED_TYPES.getName());
        Assert.assertEquals(2, p.getProperties().get(Property.ALLOWED_TYPES.getName()).size());
        Assert.assertTrue(allowedTypes.contains("HEADER"));
        Assert.assertTrue(allowedTypes.contains("TEXT"));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testNumberQuantifiers() {
        String[] quantifiers = new String[]{"{0}", "{228}", "+", "*", "?", "{2, 6}", "{3,3}"};
        int[] mins = new int[]{0, 200, 1, 0, 0, 2, 3};
        int[] maxs = new int[]{0, 200, 200, 200, 1, 6, 3};

        for (int i = 0; i < quantifiers.length; i++) {
            String quantifier = quantifiers[i];
            int min = mins[i];
            int max = maxs[i];

            String description = "__TITLE(HEADER)" + quantifier + "__";
            FieldType actual = PlaceholderUtils.parsePlaceholder(description);
            FieldType expected = PlaceholderBuilder.newBuilder("TITLE")
                    .setAllowedMinNumberOfWidgets(min)
                    .setAllowedMaxNumberOfWidgets(max)
                    .addType("HEADER")
                    .create();

            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testIllegalQuantifiers() {
        String[] quantifiers = new String[]{"!", "-4", "**", "++", "{}", "54$2@"};
        for (String quantifier : quantifiers) {
            String placeholderAsString = "__TITLE(HEADER)" + quantifier + "__";
            try {
                PlaceholderUtils.parsePlaceholder(placeholderAsString);
                Assert.fail("Expected IllegalArgumentException to be thrown from '" + placeholderAsString + "'");
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
    }

}
