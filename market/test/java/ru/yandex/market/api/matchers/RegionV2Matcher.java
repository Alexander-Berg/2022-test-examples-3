package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;

import ru.yandex.market.api.domain.Region;
import ru.yandex.market.api.domain.v2.BaseRegionV2;
import ru.yandex.market.api.domain.v2.RegionV2;
import ru.yandex.market.api.geo.domain.RegionType;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class RegionV2Matcher {
    public static <T extends RegionV2> Matcher<T> regionV2(Matcher<T>... matchers) {
        return allOf(matchers);
    }

    public static Matcher<RegionV2> id(int id) {
        return map(
            RegionV2::getId,
            "'id'",
            is(id),
            RegionV2Matcher::toStr
        );
    }

    public static Matcher<RegionV2> name(String name) {
        return map(
            RegionV2::getName,
            "'name'",
            is(name),
            RegionV2Matcher::toStr
        );
    }

    public static Matcher<RegionV2> nameRuGenitive(String nameRuGenitive) {
        return map(
            RegionV2::getNameRuGenitive,
            "'nameRuGenitive'",
            is(nameRuGenitive),
            RegionV2Matcher::toStr
        );
    }

    public static Matcher<RegionV2> nameRuAccusative(String nameRuAccusative) {
        return map(
            RegionV2::getNameRuAccusative,
            "'nameRuAccusative'",
            is(nameRuAccusative),
            RegionV2Matcher::toStr
        );
    }

    public static Matcher<RegionV2> childCount(int childCount) {
        return map(
            RegionV2::getChildCount,
            "'childCount'",
            is(childCount),
            RegionV2Matcher::toStr
        );
    }

    public static Matcher<RegionV2> countryInfo(Matcher<RegionV2> countryInfo) {
        return map(
            x -> (RegionV2) x.getCountryInfo(),
            "'countryInfo'",
            countryInfo,
            RegionV2Matcher::toStr
        );
    }

    public static Matcher<RegionV2> type(RegionType type) {
        return map(
            RegionV2::getType,
            "'type'",
            is(type),
            RegionV2Matcher::toStr
        );
    }

    public static Matcher<RegionV2> parent(Matcher<RegionV2> parent) {
        return map(
            RegionV2::getParent,
            "'parent'",
            parent,
            RegionV2Matcher::toStr
        );
    }

    public static String toStr(BaseRegionV2 regionV2) {
        if (null == regionV2) {
            return "null";
        }

        MoreObjects.ToStringHelper helper =  MoreObjects.toStringHelper(regionV2)
            .add("id", regionV2.getId())
            .add("name", regionV2.getName())
            .add("nameRuGenitive", regionV2.getNameRuGenitive())
            .add("nameRuAccusative", regionV2.getNameRuAccusative())
            .add("countryInfo", RegionV2Matcher.toStr(regionV2.getCountryInfo()));

        if (regionV2 instanceof RegionV2) {
            RegionV2 region = (RegionV2) regionV2;
            helper.add("childCount", region.getChildCount())
                    .add("type", region.getType())
                    .add("parent", RegionV2Matcher.toStr(region.getParent()));
        }

        return helper.toString();
    }
}
