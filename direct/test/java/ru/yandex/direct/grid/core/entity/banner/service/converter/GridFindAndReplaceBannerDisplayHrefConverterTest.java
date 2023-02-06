package ru.yandex.direct.grid.core.entity.banner.service.converter;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.Reflections;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.grid.core.entity.banner.model.GdiReplaceDisplayHrefBanner;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static ru.yandex.direct.core.entity.banner.repository.BannerRepositoryConstants.BANNER_CLASS_TO_TYPE;
import static ru.yandex.direct.grid.core.entity.banner.service.converter.GridFindAndReplaceBannerDisplayHrefConverter.toModelChanges;

// Проверяем работу конвертера для всех баннеров имеющих displayHref
@RunWith(Parameterized.class)
public class GridFindAndReplaceBannerDisplayHrefConverterTest {


    @Parameterized.Parameter
    public BannersBannerType bannerType;

    @Parameterized.Parameters(name = "проверка конвертации поиска и замены отображаемой ссылки баннера. тип {0}")
    public static Collection<Object[]> params() {
        Package bannerPackage = Banner.class.getPackage();
        Reflections reflections = new Reflections(bannerPackage.getName());

        List<Object[]> data = StreamEx.of(reflections.getSubTypesOf(BannerWithDisplayHref.class))
                .remove(cl -> Modifier.isAbstract(cl.getModifiers()))
                .map(b -> checkNotNull(BANNER_CLASS_TO_TYPE.get(b), "class %s must correspond to bannerType", b))
                .distinct()
                .map(type -> new Object[]{type})
                .toList();
        checkState(!data.isEmpty(), "must have at least one banner type");
        return data;
    }

    @Test
    public void checkSupportedChangeHrefConverters() {
        GdiReplaceDisplayHrefBanner gdiBanner =
                new GdiReplaceDisplayHrefBanner().withBannerType(bannerType);

        Assertions.assertThatCode(() -> toModelChanges(gdiBanner))
                .doesNotThrowAnyException();
    }

}
