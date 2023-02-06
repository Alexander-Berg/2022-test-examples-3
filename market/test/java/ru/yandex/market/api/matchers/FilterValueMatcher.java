package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.domain.v2.filters.FilterValue;
import ru.yandex.market.api.domain.v2.filters.PhotoPickerEnumValue;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class FilterValueMatcher {
    public static Matcher<FilterValue> filterValue(Matcher<FilterValue>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<FilterValue> sku(String sku) {
        return map(
                FilterValue::getSku,
                "'sku'",
                is(sku),
                FilterValueMatcher::toStr
        );
    }

    public static Matcher<FilterValue> fuzzy(boolean fuzzy) {
        return map(
                FilterValue::isFuzzy,
                "'fuzzy'",
                is(fuzzy),
                FilterValueMatcher::toStr
        );
    }

    public static Matcher<FilterValue> id(String id) {
        return map(
                FilterValue::getId,
                "'id'",
                is(id),
                FilterValueMatcher::toStr
        );
    }

    public static Matcher<FilterValue> checked(Boolean checked) {
        return map(
                FilterValue::getChecked,
                "'checked'",
                is(checked),
                FilterValueMatcher::toStr
        );
    }

    public static String toStr(FilterValue value) {
        return MoreObjects.toStringHelper(FilterValue.class)
                .add("sku", value.getSku())
                .add("fuzzy", String.valueOf(value.isFuzzy()))
                .add("id", value.getId())
                .add("checked", value.getChecked())
                .toString();
    }
}
