package ru.yandex.market.personal;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith(SoftAssertionsExtension.class)
public class AbstractTest {

    protected static final String DISPLAY_NAME_INDEX_PLACEHOLDER = "[" + INDEX_PLACEHOLDER + "] {0}";

    @InjectSoftAssertions
    protected SoftAssertions softly;
}
