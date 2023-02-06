package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.domain.v2.option.AvailableReportSort;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.model.UniversalModelSort.SortField;

import static org.hamcrest.Matchers.*;
import static ru.yandex.market.api.ApiMatchers.collectionToStr;
import static ru.yandex.market.api.ApiMatchers.map;

public class ReportSortsMatcher {
    public static Matcher<AvailableReportSort> sort(Matcher<AvailableReportSort> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<AvailableReportSort> text(String text) {
        return map(
            AvailableReportSort::getText,
            "'text'",
            is(text),
            ReportSortsMatcher::toStr
        );
    }

    public static Matcher<AvailableReportSort> field(SortField field) {
        return map(
            AvailableReportSort::getField,
            "'field'",
            is(field),
            ReportSortsMatcher::toStr
        );
    }

    public static Matcher<AvailableReportSort> options(Matcher<AvailableReportSort.Option> ... options) {
        return map(
            AvailableReportSort::getOptions,
            "'options'",
            hasItems(options),
            ReportSortsMatcher::toStr
        );
    }

    public static Matcher<AvailableReportSort.Option> option(Matcher<AvailableReportSort.Option> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<AvailableReportSort.Option> optionId(String optionId) {
        return map(
            AvailableReportSort.Option::getId,
            "'id'",
            is(optionId),
            ReportSortsMatcher::toStr
        );
    }

    public static Matcher<AvailableReportSort.Option> optionHow(SortOrder how) {
        return map(
            AvailableReportSort.Option::getHow,
            "'how'",
            is(how),
            ReportSortsMatcher::toStr
        );
    }

    public static Matcher<AvailableReportSort.Option> optionText(String text) {
        return map(
            AvailableReportSort.Option::getText,
            "'text'",
            is(text),
            ReportSortsMatcher::toStr
        );
    }


    private static String toStr(AvailableReportSort sort) {
        if (null == sort) {
            return "null";
        }
        return MoreObjects.toStringHelper("Sort")
            .add("text", sort.getText())
            .add("field", sort.getField())
            .add("options", collectionToStr(sort.getOptions(),ReportSortsMatcher::toStr))
            .toString();
    }

    private static String toStr(AvailableReportSort.Option option) {
        if (null == option) {
            return "null";
        }
        return MoreObjects.toStringHelper(AvailableReportSort.Option.class)
            .add("id", option.getId())
            .add("how", option.getHow())
            .add("text", option.getText())
            .toString();
    }
}
