package ru.yandex.market.logistics.validation;

import javax.validation.Validation;
import javax.validation.Validator;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith(SoftAssertionsExtension.class)
public class AbstractTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    protected final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @InjectSoftAssertions
    protected SoftAssertions softly;
}