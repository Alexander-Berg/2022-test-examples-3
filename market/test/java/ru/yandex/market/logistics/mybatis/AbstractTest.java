package ru.yandex.market.logistics.mybatis;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AbstractTest {
    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();
}
