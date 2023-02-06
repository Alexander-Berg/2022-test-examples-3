package ru.yandex.market.logistics.management;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

public abstract class AbstractTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();
}
