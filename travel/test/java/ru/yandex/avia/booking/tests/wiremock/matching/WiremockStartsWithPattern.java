package ru.yandex.avia.booking.tests.wiremock.matching;

import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

public class WiremockStartsWithPattern extends StringValuePattern {
    public WiremockStartsWithPattern(String value) {
        super(value);
    }

    @Override
    public MatchResult match(String value) {
        return MatchResult.of(value.startsWith(getValue()));
    }
}
