package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.domain.v2.filters.EnumFilter;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.domain.v2.filters.FilterValue;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.collectionToStr;
import static ru.yandex.market.api.ApiMatchers.map;


public class FiltersMatcher {
    public static Matcher<Filter> filter(Matcher<Filter> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<Filter> id(String id) {
        return map(
            Filter::getId,
            "'id'",
            is(id),
            FiltersMatcher::toStr
        );
    }

    public static Matcher<Filter> type(String type) {
        return map(
            Filter::getType,
            "'type'",
            is(type),
            FiltersMatcher::toStr
        );
    }

    public static Matcher<Filter> subType(String subType) {
        return map(
            Filter::getSubtype,
            "'subType'",
            is(subType),
            FiltersMatcher::toStr
        );
    }

    public static Matcher<Filter> name(String name) {
        return map(
            Filter::getName,
            "'name'",
            is(name),
            FiltersMatcher::toStr
        );
    }

    public static Matcher<Filter> values(Matcher<Iterable<? extends FilterValue>> values) {
        return (Matcher<Filter>) (Object) map(
            EnumFilter::getValues,
            "'values'",
            values,
            FiltersMatcher::toStr
        );
    }

    public static String toStr(Filter filter) {
        if (null == filter) {
            return "null";
        }
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(Filter.class)
            .add("id", filter.getId())
            .add("type", filter.getType())
            .add("subType", filter.getSubtype())
            .add("name", filter.getName())
            .add("description", filter.getDescription());
        return addValues(helper, filter)
                .toString();

    }

    private static MoreObjects.ToStringHelper addValues(MoreObjects.ToStringHelper helper,
                                                        Filter filter) {
        if (filter instanceof EnumFilter) {
            helper.add(
                "values",
                collectionToStr(
                    ((EnumFilter) filter).getValues(),
                    FilterValueMatcher::toStr
                )
            );
        }

        return helper;
    }
}
