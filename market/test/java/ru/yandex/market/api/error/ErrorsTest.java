package ru.yandex.market.api.error;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author Denis Chernyshov
 */
public class ErrorsTest extends UnitTestBase {

    @Test
    public void shouldReturnEmptyStringWhenNoParameters() {
        Assert.assertEquals("", Errors.getLogErrorMessage());
    }

    @Test
    public void shouldReturnNULLWhenNullParameter() {
        Assert.assertEquals("NULL", Errors.getLogErrorMessage(new Object[] {null}));
    }

    @Test
    public void shouldReturnTwoNULLWhenTwoNullParameter() {
        Assert.assertEquals("NULL, NULL", Errors.getLogErrorMessage(null, null));
    }

    @Test
    public void shouldReturnEmptyQuotedStringWhenEmptyString() {
        Assert.assertEquals("\"\"", Errors.getLogErrorMessage(""));
    }

    @Test
    public void shouldReturnEmptyQuotedStringsWhenEmptyStrings() {
        Assert.assertEquals("\"\", \"\", \"\"", Errors.getLogErrorMessage("", "", ""));

    }

    @Test
    public void shouldReturnStringsWhenEmptyOrNullStrings() {
        Assert.assertEquals("\"\", NULL, \"\", NULL", Errors.getLogErrorMessage("", null, "", null));
        Assert.assertEquals("NULL, \"\", NULL, \"\"", Errors.getLogErrorMessage(null, "", null, ""));
    }

    @Test
    public void shouldReturnTagOnTag() {
        Assert.assertEquals("#tag", Errors.getLogErrorMessage("#tag"));
    }

    @Test
    public void shouldReturnTags() {
        Assert.assertEquals("#tag1, #tag2", Errors.getLogErrorMessage("#tag1", "#tag2"));
    }

    @Test
    public void shouldReturnTagAndParamsOnTags() {
        Assert.assertEquals("#tag1, NULL, #tag2", Errors.getLogErrorMessage("#tag1", null, "#tag2"));
    }

    @Test
    public void shouldReturnTagsAndParams() {
        Assert.assertEquals("#tag1, NULL, \"\", #tag2", Errors.getLogErrorMessage("#tag1", null, "", "#tag2"));
    }

    @Test
    public void shouldReturnKey() {
        Assert.assertEquals("KEY:", Errors.getLogErrorMessage("KEY:"));
    }

    @Test
    public void shouldKeyAndValue() {
        Assert.assertEquals("KEY: value", Errors.getLogErrorMessage("KEY:", "value"));
    }

    @Test
    public void shouldKeyAndNULL() {
        Assert.assertEquals("KEY: NULL", Errors.getLogErrorMessage("KEY:", null));
    }

    @Test
    public void shouldKeyAndEmptyString() {
        Assert.assertEquals("KEY: \"\"", Errors.getLogErrorMessage("KEY:", ""));
    }

    @Test
    public void shouldKeyAndSpacedString() {
        Assert.assertEquals("KEY: \" \"", Errors.getLogErrorMessage("KEY:", " "));
    }

    @Test
    public void shouldKeyAndNewLinedString() {
        Assert.assertEquals("KEY: \\n", Errors.getLogErrorMessage("KEY:", "\n"));
        Assert.assertEquals("KEY: \\r", Errors.getLogErrorMessage("KEY:", "\r"));
    }

    @Test
    public void shouldKeyAndSlashedString() {
        Assert.assertEquals("KEY: \\\\", Errors.getLogErrorMessage("KEY:", "\\"));
        Assert.assertEquals("KEY: \\\\n", Errors.getLogErrorMessage("KEY:", "\\n"));
    }

    @Test
    public void shouldReturnKeyAndQuotedString() {
        Assert.assertEquals("KEY: \"1 2\"", Errors.getLogErrorMessage("KEY:", "1 2"));
    }

    @Test
    public void shouldCombineWell() {
        Assert.assertEquals("KEY: value", Errors.getLogErrorMessage("KEY:", "value"));
        Assert.assertEquals("KEY: value, KEY1: \"v a l u e\"", Errors.getLogErrorMessage("KEY:", "value", "KEY1:", "v a l u e"));
        Assert.assertEquals("#tag1, KEY: value, #tag2, #tag3", Errors.getLogErrorMessage("#tag1", "KEY:", "value", "#tag2", "#tag3"));
        Assert.assertEquals("#tag1, KEY: value, KEY1: value\\nnew_line, #tag2, #tag3",
                Errors.getLogErrorMessage("#tag1", "KEY:", "value", "KEY1:", "value\nnew_line", "#tag2", "#tag3"));
    }
}