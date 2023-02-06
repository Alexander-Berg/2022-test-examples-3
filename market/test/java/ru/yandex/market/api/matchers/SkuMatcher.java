package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.FormattedDescription;
import ru.yandex.market.api.domain.v2.ImageWithThumbnails;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.domain.v2.WarningInfo;
import ru.yandex.market.api.domain.v2.filters.Filter;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class SkuMatcher {
    public static Matcher<Sku> sku(Matcher<Sku>... matchers) {
        return allOf(matchers);
    }

    public static Matcher<Sku> id(String id) {
        return map(
            Sku::getId,
            "'id'",
            is(id),
            SkuMatcher::toStr
        );
    }

    public static Matcher<Sku> deletedId(String id) {
        return map(
                Sku::getDeletedId,
                "'deletedId'",
                is(id),
                SkuMatcher::toStr
        );
    }


    public static Matcher<Sku> name(String name) {
        return map(
            Sku::getName,
            "'name'",
            is(name),
            SkuMatcher::toStr
        );
    }

    public static Matcher<Sku> description(String description) {
        return map(
            Sku::getDescription,
            "'description'",
            is(description),
            SkuMatcher::toStr
        );
    }

    public static Matcher<Sku> photos(Matcher<Iterable<? extends ImageWithThumbnails>> photos) {
        return map(
            Sku::getPhotos,
            "'photos'",
            photos,
            SkuMatcher::toStr
        );
    }

    public static Matcher<Sku> model(Matcher<ModelV2> ... model) {
        return map(
            Sku::getModel,
            "'model'",
            Matchers.allOf(model),
            SkuMatcher::toStr
        );

    }

    public static Matcher<Sku> offers(Matcher<Iterable<? extends OfferV2>> offers) {
        return map(
            Sku::getOffers,
            "'offers'",
            offers,
            SkuMatcher::toStr
        );
    }

    public static Matcher<Sku> filters(Matcher<Iterable<? extends Filter>> filters) {
        return  map(
            Sku::getFilters,
            "'filters'",
            filters,
            SkuMatcher::toStr
        );
    }

    public static Matcher<Sku> formattedDescription(Matcher<FormattedDescription> ... formattedDescription) {
        return map(
                Sku::getFormattedDescription,
                "'formattedDescription'",
                allOf(formattedDescription),
                SkuMatcher::toStr
        );

    }

    public static Matcher<Sku> warnings(Matcher<Iterable<? extends WarningInfo>> warnings) {
        return map(
            Sku::getWarnings,
            "'warnings'",
            warnings,
            SkuMatcher::toStr
        );

    }

    public static Matcher<Sku> cms(Matcher<String> cms) {
        return map(
            Sku::getCms,
            "'cms'",
            cms,
            SkuMatcher::toStr
        );
    }

    public static Matcher<Sku> skuType(String skuType) {
        return map(
            Sku::getSkuType,
            "'skuType'",
            is(skuType),
            SkuMatcher::toStr
        );
    }

    private static String toStr(Sku sku) {
        if (null == sku) {
            return "null";
        }
        return MoreObjects.toStringHelper(Sku.class)
            .add("id", sku.getId())
            .add("name", sku.getName())
            .add("description", sku.getDescription())
            .add(
                "fomrattedDescription",
                FormattedDescriptionMatcher.toStr(sku.getFormattedDescription())
            )
            .add(
                "photos",
                ApiMatchers.collectionToStr(sku.getPhotos(), ImageWithThumbnails::toString)
            )
            .add("model", ModelMatcher.toStr(sku.getModel()))
            .add(
                "offers",
                ApiMatchers.collectionToStr(sku.getOffers(), OfferMatcher::toStr)
            )
            .add(
                "filters",
                ApiMatchers.collectionToStr(sku.getFilters(), FiltersMatcher::toStr)
            )
            .add("skuType", sku.getSkuType())
            .toString();
    }
}
