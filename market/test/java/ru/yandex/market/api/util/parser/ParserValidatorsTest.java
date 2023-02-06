package ru.yandex.market.api.util.parser;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.UnitTestBase;

/**
 * Created by tesseract on 14.04.14.
 */
public class ParserValidatorsTest extends UnitTestBase {

    ValidationErrors errors;

    @Test
    public void gtOrEq() {
        ParseValidator<Integer> validator = ParseValidators.gtOrEq(-2, "tmpl %s", "arg");
        Assert.assertTrue(validator.validate(-2, errors));
        Assert.assertEquals(0, errors.size());
        Assert.assertTrue(validator.validate(1, errors));
        Assert.assertEquals(0, errors.size());
        Assert.assertFalse(validator.validate(-3, errors));
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void rangeWithError() {
        ParseValidator<Integer> validator = ParseValidators.range(-2, 3, "tmpl %s", "arg");

        Assert.assertTrue(validator.validate(-2, errors));
        Assert.assertEquals(0, errors.size());
        Assert.assertTrue(validator.validate(1, errors));
        Assert.assertEquals(0, errors.size());
        Assert.assertTrue(validator.validate(3, errors));
        Assert.assertEquals(0, errors.size());
        Assert.assertFalse(validator.validate(-3, errors));
        Assert.assertEquals(1, errors.size());
        Assert.assertFalse(validator.validate(4, errors));
        Assert.assertEquals(2, errors.size());
    }

    @Before
    public void setUp() {
        errors = new ValidationErrors();
    }

    @Test
    public void simpleRange() {
        ParseValidator<Integer> validator = ParseValidators.range(-2, 3);

        Assert.assertTrue(validator.validate(-2, errors));
        Assert.assertTrue(validator.validate(1, errors));
        Assert.assertTrue(validator.validate(3, errors));
        Assert.assertFalse(validator.validate(-3, errors));
        Assert.assertFalse(validator.validate(4, errors));
    }
}
