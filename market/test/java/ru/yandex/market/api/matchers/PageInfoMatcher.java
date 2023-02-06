package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.domain.PageInfo;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class PageInfoMatcher {
    public static Matcher<PageInfo> pageInfo(Matcher<PageInfo> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<PageInfo> page(int page) {
        return map(
            PageInfo::getPage,
            "'page'",
            is(page),
            PageInfoMatcher::toStr
        );
    }

    public static Matcher<PageInfo> count(int count) {
        return map(
            PageInfo::getCount,
            "'count'",
            is(count),
            PageInfoMatcher::toStr
        );
    }

    public static Matcher<PageInfo> totalPages(int totalPages) {
        return map(
            PageInfo::getTotalPages,
            "'totalPages'",
            is(totalPages),
            PageInfoMatcher::toStr
        );
    }

    private static String toStr(PageInfo pageInfo) {
        if (null == pageInfo) {
            return "null";
        }
        return MoreObjects.toStringHelper(PageInfo.class)
            .add("page", pageInfo.getPage())
            .add("count", pageInfo.getCount())
            .add("totalPages", pageInfo.getTotalPages())
            .toString();
    }
}

