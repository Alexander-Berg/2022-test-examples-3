package ru.yandex.market.pdf.generator;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class BaseTest {
    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();
}
