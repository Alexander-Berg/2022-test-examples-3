package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.internal.adfox.AdfoxAttributes;

import java.util.List;
import java.util.Map;

public class AdfoxMatcher {
    public static Matcher<List<? extends AdfoxAttributes>> adfox(
        Matcher<Iterable<? extends AdfoxAttributes>> ... matchers) {
        return CoreMatchers.allOf(matchers);
    }

    public static Matcher<AdfoxAttributes> attribute(String content) {
        return ApiMatchers.map(
            AdfoxAttributes::getContent,
            "'content'",
            Matchers.is(content)
        );
    }

    public static String toStr(AdfoxAttributes attributes) {
        if (null == attributes) {
            return "null";
        }
        return MoreObjects.toStringHelper(AdfoxAttributes.class)
            .add("content", attributes.getContent())
            .toString();
    }
}
