package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.Organization;
import ru.yandex.market.api.domain.v2.ShopInfoV2;

public class ShopInfoMatcher {
    public static Matcher<ShopInfoV2> shop(Matcher<ShopInfoV2> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<ShopInfoV2> id(long id) {
        return ApiMatchers.map(
            ShopInfoV2::getId,
            "'id'",
            Matchers.is(id),
            ShopInfoMatcher::toStr
        );
    }

    public static Matcher<ShopInfoV2> organizations(Matcher<? super Iterable<Organization>> orgs) {
        return ApiMatchers.map(
            ShopInfoV2::getOrganizations,
            "'organizations'",
            orgs
        );
    }

    public static String toStr(ShopInfoV2 shop) {
        if (null == shop) {
            return "null";
        }

        return MoreObjects.toStringHelper("Shop")
            .add("id", shop.getId())
            .add(
                "organizations",
                ApiMatchers.collectionToStr(
                    shop.getOrganizations(),
                    OrganizationMatcher::toStr
                )
            )
            .toString();
    }
}
