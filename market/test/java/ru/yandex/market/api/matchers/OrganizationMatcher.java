package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.Organization;
import ru.yandex.market.api.shop.OrganizationType;

public class OrganizationMatcher {
    public static Matcher<Organization> organization(Matcher<Organization>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<Organization> name(String name) {
        return ApiMatchers.map(
          Organization::getName,
          "'name'",
          Matchers.is(name),
          OrganizationMatcher::toStr
        );
    }

    public static Matcher<Organization> orgn(String orgn) {
        return ApiMatchers.map(
            Organization::getOgrn,
            "'orgn'",
            Matchers.is(orgn),
            OrganizationMatcher::toStr
        );
    }

    public static Matcher<Organization> type(OrganizationType type) {
        return ApiMatchers.map(
            Organization::getType,
            "'type'",
            Matchers.is(type),
            OrganizationMatcher::toStr
        );
    }


    public static String toStr(Organization org) {
        if (null == org) {
            return "null";
        }
        return MoreObjects.toStringHelper(org)
            .add("name", org.getName())
            .add("ogrn", org.getOgrn())
            .add("type", org.getType())
            .toString();
    }
}
