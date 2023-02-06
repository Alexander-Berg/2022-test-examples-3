package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.FormattedDescription;

import static org.hamcrest.Matchers.allOf;

public class FormattedDescriptionMatcher {

    public static Matcher<FormattedDescription> formattedDescription(Matcher<FormattedDescription>... matchers) {
        return allOf(matchers);
    }

    public static <T extends FormattedDescription> Matcher<T> fullPlain(String fullPlain) {
        return ApiMatchers.map(
                FormattedDescription::getFullPlain,
                "'fullPlain'",
                Matchers.is(fullPlain),
                FormattedDescriptionMatcher::toStr
        );
    }

    public static <T extends FormattedDescription> Matcher<T> shortPlain(String shortPlain) {
        return ApiMatchers.map(
                FormattedDescription::getShortPlain,
                "'shortPlain'",
                Matchers.is(shortPlain),
                FormattedDescriptionMatcher::toStr
        );
    }

    public static <T extends FormattedDescription> Matcher<T> fullHtml(String fullHtml) {
        return ApiMatchers.map(
                FormattedDescription::getFullHtml,
                "'fullHtml'",
                Matchers.is(fullHtml),
                FormattedDescriptionMatcher::toStr
        );
    }

    public static <T extends FormattedDescription> Matcher<T> shortHtml(String shortHtml) {
        return ApiMatchers.map(
                FormattedDescription::getShortHtml,
                "'shortHtml'",
                Matchers.is(shortHtml),
                FormattedDescriptionMatcher::toStr
        );
    }

    public static  String toStr(FormattedDescription formattedDescription) {
        if (null == formattedDescription) {
            return "null";
        }
        return MoreObjects.toStringHelper(FormattedDescription.class)
            .add("fullPlain", formattedDescription.getFullPlain())
            .add("shortPlain", formattedDescription.getShortPlain())
            .add("fullHtml", formattedDescription.getFullHtml())
            .add("shortHtml", formattedDescription.getShortHtml())
            .toString();
    }

}
