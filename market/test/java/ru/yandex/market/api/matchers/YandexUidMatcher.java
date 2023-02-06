package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.server.sec.YandexUid;

/**
 * @author dimkarp93
 */
public class YandexUidMatcher {
    public static Matcher<YandexUid> yandexUid(String yandexUid) {
        return ApiMatchers.map(
            YandexUid::getValue,
            "'yandexUid'",
            Matchers.is(yandexUid),
            YandexUidMatcher::toStr
        );
    }

    public static String toStr(YandexUid yandexUid) {
        if (null == yandexUid) {
            return "null";
        }
        return MoreObjects.toStringHelper(YandexUid.class)
            .add("value", yandexUid.getValue())
            .toString();
    }

}
