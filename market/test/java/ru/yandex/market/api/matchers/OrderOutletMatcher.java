package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.Outlet;

/**
 * Created by fettsery on 17.07.18.
 */
public class OrderOutletMatcher {
    public static Matcher<Outlet> outlet(Matcher<Outlet> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<Outlet> id(long id) {
        return ApiMatchers.map(
            Outlet::getId,
            "'id'",
            Matchers.is(id),
            OrderOutletMatcher::toStr
        );
    }

    public static Matcher<Outlet> name(String name) {
        return ApiMatchers.map(
            Outlet::getName,
            "'name'",
            Matchers.is(name),
            OrderOutletMatcher::toStr
        );
    }

    public static String toStr(Outlet outlet) {
        if (null == outlet) {
            return "null";
        }
        return MoreObjects.toStringHelper(Outlet.class)
            .add("id", outlet.getId())
            .add("name", outlet.getName())
            .toString();
    }
}
