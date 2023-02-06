package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.domain.v2.promo.PromoLink;

import static org.hamcrest.Matchers.is;

public class PromoLinkMatcher {
    public static Matcher<PromoLink> promoLink(Matcher<PromoLink> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<PromoLink> image(Matcher<Image> image) {
        return ApiMatchers.map(
            PromoLink::getImage,
            "'image'",
            image,
            PromoLinkMatcher::toStr
        );
    }

    public static Matcher<PromoLink> url(String url) {
        return ApiMatchers.map(
            PromoLink::getUrl,
            "'url'",
            is(url),
            PromoLinkMatcher::toStr
        );
    }

    public static String toStr(PromoLink promoLink) {
        if (null == promoLink) {
            return "null";
        }
        return MoreObjects.toStringHelper(PromoLink.class)
            .add("image", ImageMatcher.toStr(promoLink.getImage()))
            .add("url", promoLink.getUrl())
            .toString();
    }
}
