package ru.yandex.market.apt;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AbstractTest {
    @RegisterExtension
    protected JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();
}
