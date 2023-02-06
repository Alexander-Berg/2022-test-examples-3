package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.domain.Model;
import ru.yandex.market.api.domain.Offer;
import ru.yandex.market.api.domain.v2.ImageWithThumbnails;
import ru.yandex.market.api.domain.v2.ModelSkuStats;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.internal.guru.ModelType;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class ModelMatcher {
    public static <T extends Model> Matcher<T> model(Matcher<T>... matchers) {
        return allOf(matchers);
    }

    public static Matcher<ModelV2> id(long id) {
        return map(
            ModelV2::getId,
            "'id'",
            is(id),
            ModelMatcher::toStr
        );
    }

    public static Matcher<ModelV2> name(String name) {
        return map(
                ModelV2::getName,
                "'name'",
                is(name)
        );
    }

    public static Matcher<ModelV2> description(String description) {
        return map(
                ModelV2::getDescription,
                "'description'",
                is(description)
        );
    }

    public static Matcher<ModelV2> isNew(Boolean isNew) {
        return map(
                ModelV2::getIsNew,
                "'isNew'",
                is(isNew)
        );
    }

    public static Matcher<ModelV2> kind(String kind) {
        return map(
                ModelV2::getKind,
                "'kind'",
                is(kind)
        );
    }

    public static Matcher<ModelV2> type(ModelType type) {
        return map(
                ModelV2::getType,
                "'type'",
                is(type)
        );
    }

    public static Matcher<ModelV2> offerCount(Long offerCount) {
        return map(
                ModelV2::getOfferCount,
                "'type'",
                is(offerCount)
        );
    }

    public static Matcher<ModelV2> offer(Matcher<Offer> ... offer) {
        return map(
            ModelV2::getOffer,
            "'offer'",
            allOf(offer),
            ModelMatcher::toStr
        );
    }

    public static Matcher<ModelV2> photos(Matcher<Iterable<? extends ImageWithThumbnails>> ... photos) {
        return map(
            x -> (Iterable<? extends ImageWithThumbnails>) x.getPhotos(),
            "'photos'",
            allOf(photos),
            ModelMatcher::toStr
        );
    }

    public static Matcher<ModelV2> photosCount(int size) {
        return map(
                ModelV2::getPhotos,
                "'photos'",
                hasSize(size),
                ModelMatcher::toStr
        );
    }

    public static Matcher<ModelV2> skuStats(Matcher<ModelSkuStats> ... modelSkuStats) {
        return map(
            ModelV2::getSkuStats,
            "'skuStats'",
            allOf(modelSkuStats),
            ModelMatcher::toStr
        );
    }

    public static String toStr(ModelV2 model) {
        if (null == model) {
            return "null";
        }
        return MoreObjects.toStringHelper(ModelV2.class)
            .add("model", model.getId())
            .add("offer", OfferMatcher.toStr((OfferV2) model.getOffer()))
            .add("skuStats", ModelSkuStatsMatcher.toStr(model.getSkuStats()))
            .toString();
    }
}
