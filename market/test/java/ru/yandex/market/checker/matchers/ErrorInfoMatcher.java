package ru.yandex.market.checker.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

public class ErrorInfoMatcher {
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
