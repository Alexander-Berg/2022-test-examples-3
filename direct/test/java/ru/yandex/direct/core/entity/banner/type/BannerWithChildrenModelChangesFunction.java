package ru.yandex.direct.core.entity.banner.type;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;

@FunctionalInterface
public interface BannerWithChildrenModelChangesFunction<T> {

    ModelChanges<BannerWithSystemFields> getModelChanges(
            Steps steps, ClientInfo clientInfo,
            Class<? extends T> clazz, Long bannerId);
}
