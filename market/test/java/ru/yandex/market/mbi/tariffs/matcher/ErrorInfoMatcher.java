package ru.yandex.market.mbi.tariffs.matcher;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

/**
 * Матчер для {@link ErrorInfo}
 */
public final class ErrorInfoMatcher {
    private ErrorInfoMatcher() {
        throw new UnsupportedOperationException();
    }

    public static Matcher<ErrorInfo> hasCode(String expectedValue) {
        return MbiMatchers.<ErrorInfo>newAllOfBuilder()
            .add(ErrorInfo::getCode, expectedValue, "code")
            .build();
    }

    public static Matcher<ErrorInfo> hasMessage(String expectedValue) {
        return MbiMatchers.<ErrorInfo>newAllOfBuilder()
            .add(ErrorInfo::getMessage, expectedValue, "message")
            .build();
    }

    public static Matcher<ErrorInfo> hasStackTrace(String expectedValue) {
        return MbiMatchers.<ErrorInfo>newAllOfBuilder()
                .add(ErrorInfo::getStackTrace, expectedValue, "stactrace")
                .build();
    }
}
