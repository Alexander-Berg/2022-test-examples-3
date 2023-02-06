package ru.yandex.market.api.domain.v2.criterion;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 27.01.17.
 */
public class TextCriterionTest extends UnitTestBase {

    @Test
    public void testReturnUnknownLiteralAsOriginal() {
        TextCriterion criterion = new TextCriterion("", "unknown:\"123123\"", null);
        assertEquals("unknown:\"123123\"", criterion.getText());
    }

    @Test
    public void testProcessLiteral() {
        TextCriterion criterion = new TextCriterion("", "barcode:\"123123\"", null);
        assertEquals("123123", criterion.getText());
    }

}