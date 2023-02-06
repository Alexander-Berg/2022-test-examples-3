package ru.yandex.market.api.util.parser2.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.category.CategoryService;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.util.parser2.validation.errors.CategoryIdValidationError;
import ru.yandex.market.api.util.parser2.validation.errors.ParsedValueValidationError;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by fettsery on 28.01.19.
 */
public class CategoryIdValidatorTest extends ContainerTestBase {
    private CategoryIdValidator categoryIdValidator;

    @Autowired
    private CategoryService categoryService;

    @Override
    @Before
    public void setUp() throws Exception {
        categoryIdValidator = CategoryIdValidator.createDefault(categoryService, true);
        super.setUp();
    }

    @Test
    public void shouldValidateZeroHid() {
        Assert.assertNull(categoryIdValidator.validate(Maybe.just(0)));
    }

    @Test
    public void shouldValidateEmptyHid() {
        Assert.assertNull(categoryIdValidator.validate(Maybe.nothing()));
    }

    @Test
    public void shouldValidateExistentHid() {
        Assert.assertNull(categoryIdValidator.validate(Maybe.just(91491)));
    }

    @Test
    public void shouldNotValidateNotExistentHid() {
        ParsedValueValidationError error = categoryIdValidator.validate(Maybe.just(5050));
        Assert.assertTrue(error instanceof CategoryIdValidationError);
    }

    @Test
    public void shouldNotValidateExcludedHid() {
        Assert.assertThat(categoryIdValidator.validate(Maybe.just(-91491)), notNullValue());
    }

    @Test
    public void shouldValidateExcludedHid() {
        categoryIdValidator = CategoryIdValidator.createWithExclusions(categoryService, true);
        Assert.assertNull(categoryIdValidator.validate(Maybe.just(-91491)));
    }

    @Test
    public void shouldNotValidateIfDisabled() {
        categoryIdValidator = CategoryIdValidator.createDefault(categoryService, false);
        Assert.assertNull(categoryIdValidator.validate(Maybe.just(91491)));
        Assert.assertNull(categoryIdValidator.validate(Maybe.just(5050)));
    }
}
