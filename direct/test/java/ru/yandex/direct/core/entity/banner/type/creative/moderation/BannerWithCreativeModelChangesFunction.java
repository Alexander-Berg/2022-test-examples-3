package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.type.BannerWithChildrenModelChangesFunction;

@FunctionalInterface
public interface BannerWithCreativeModelChangesFunction
        extends BannerWithChildrenModelChangesFunction<BannerWithCreative> {
}
