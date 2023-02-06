package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;

import ru.yandex.market.api.domain.v2.BaseRegionV2;
import ru.yandex.market.api.domain.v2.GeoSuggest;
import ru.yandex.market.api.domain.v2.RegionV2;
import ru.yandex.market.api.geo.domain.RegionType;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class GeoSuggestMatcher {

    public static <T extends GeoSuggest> Matcher<T> suggest(Matcher<T>... matchers) {
        return allOf(matchers);
    }

    public static Matcher<GeoSuggest> id(int id) {
        return map(
            GeoSuggest::getId,
            "'id'",
            is(id),
            GeoSuggestMatcher::toStr
        );
    }

    public static Matcher<GeoSuggest> name(String name) {
        return map(
            GeoSuggest::getName,
            "'name'",
            is(name),
            GeoSuggestMatcher::toStr
        );
    }

    public static Matcher<GeoSuggest> type(RegionType type) {
        return map(
            GeoSuggest::getType,
            "'type'",
            is(type),
            GeoSuggestMatcher::toStr
        );
    }

    public static Matcher<GeoSuggest> childCount(int childCount) {
        return map(
            GeoSuggest::getChildCount,
            "'childCount'",
            is(childCount),
            GeoSuggestMatcher::toStr
        );
    }

    public static Matcher<GeoSuggest> countryInfo(Matcher<RegionV2> countryInfo) {
        return map(
            x -> (RegionV2) x.getCountryInfo(),
            "'countryInfo'",
            countryInfo,
            GeoSuggestMatcher::toStr
        );
    }

    public static Matcher<GeoSuggest> parent(Matcher<RegionV2> parent) {
        return map(
            GeoSuggest::getParent,
            "'parent'",
            parent,
            GeoSuggestMatcher::toStr
        );
    }

    public static Matcher<GeoSuggest> fullName(String fullName) {
        return map(
            GeoSuggest::getFullName,
            "'fullName'",
            is(fullName),
            GeoSuggestMatcher::toStr
        );
    }

    public static String toStr(GeoSuggest suggest) {
        if (null == suggest) {
            return "null";
        }
        return MoreObjects.toStringHelper(suggest)
            .add("id", suggest.getId())
            .add("name", suggest.getName())
            .add("fullName", suggest.getFullName())
            .add("type", suggest.getType())
            .add("childCount", suggest.getChildCount())
            .add("countryInfo", RegionV2Matcher.toStr(suggest.getCountryInfo()))
            .add("parent", RegionV2Matcher.toStr(suggest.getParent()))
            .toString();
    }
}
