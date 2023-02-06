package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.domain.v2.criterion.TextCriterion;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class CriterionMatcher {
    public static Matcher<Criterion> criterion(String id, String value) {
        return allOf(
            map(
                Criterion::getId,
                "'id'",
                is(id),
                CriterionMatcher::toStr
            ),
            map(
                Criterion::getValue,
                "'value'",
                is(value),
                CriterionMatcher::toStr
            )
        );
    }

    public static Matcher<TextCriterion> textCriterion(String value, String text, String reportState) {
        return allOf(
            map(
                TextCriterion::getValue,
                "'value'",
                is(value),
                CriterionMatcher::toStr
            ),
            map(
                TextCriterion::getText,
                "'text'",
                is(text),
                CriterionMatcher::toStr
            ),
            map(
                TextCriterion::getReportState,
                "'reportState'",
                is(reportState),
                CriterionMatcher::toStr
            )
        );
    }

    public static String toStr(Criterion criterion) {
        if (null == criterion) {
            return "null";
        }
        return MoreObjects.toStringHelper(Criterion.class)
            .add("id", criterion.getId())
            .add("value", criterion.getValue())
            .toString();
    }
}
