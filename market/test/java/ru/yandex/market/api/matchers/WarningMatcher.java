package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.*;
import ru.yandex.market.api.domain.v2.filters.Filter;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class WarningMatcher {
    public static Matcher<WarningInfo> warnings(Matcher<WarningInfo>... matchers) {
        return allOf(matchers);
    }

    public static Matcher<WarningInfo> code(String code) {
        return map(
            WarningInfo::getCode,
            "'code'",
            is(code),
            WarningMatcher::toStr
        );
    }


    public static Matcher<WarningInfo> text(String text) {
        return map(
            WarningInfo::getText,
            "'warnings'",
            is(text),
            WarningMatcher::toStr
        );

    }

    private static String toStr(WarningInfo warning) {
        if (null == warning) {
            return "null";
        }
        return MoreObjects.toStringHelper(WarningInfo.class)
            .add("code", warning.getCode())
            .add("text", warning.getText())
            .toString();
    }
}
