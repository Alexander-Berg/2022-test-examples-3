package ru.yandex.direct.grid.core.entity.banner.service.converter;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.Reflections;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdImageModerationInfo;
import ru.yandex.direct.core.entity.banner.model.BannerWithInternalAdModerationInfo;
import ru.yandex.direct.core.entity.banner.model.BannerWithModerationInfo;
import ru.yandex.direct.core.entity.banner.model.BannerWithOutdoorModerationInfo;
import ru.yandex.direct.core.entity.banner.model.BannerWithTextAndImageModerationInfo;
import ru.yandex.direct.core.entity.banner.model.StubBanner;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItem;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static ru.yandex.direct.core.entity.banner.repository.BannerRepositoryConstants.BANNER_CLASS_TO_TYPE;
import static ru.yandex.direct.grid.core.entity.banner.service.converter.GridFindAndReplaceBannerHrefConverter.gdiBannerHrefToModelChanges;

// для всех баннеров, у которых есть ссылка и операция обновления, определяем конвертер
@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GridFindAndReplaceBannerHrefConverterToModelChangesTest {

    private static final Set<BannersBannerType> UNSUPPORTED_BANNER_HREF_TYPES =
            ImmutableSet.of(BannersBannerType.internal, BannersBannerType.performance,
                    BannersBannerType.performance_main, BannersBannerType.cpm_outdoor,
                    BannersBannerType.cpm_indoor, BannersBannerType.cpm_audio, BannersBannerType.content_promotion,
                    BannersBannerType.cpm_geo_pin);

    private static final Set<Class<? extends Banner>> UNSUPPORTED_BANNER_CLASSES =
            Set.of(Banner.class, StubBanner.class, BannerWithAdImageModerationInfo.class,
                    BannerWithModerationInfo.class, BannerWithOutdoorModerationInfo.class,
                    BannerWithInternalAdModerationInfo.class, BannerWithTextAndImageModerationInfo.class);

    @Parameterized.Parameter
    public BannersBannerType bannerType;

    @Parameterized.Parameters(name = "проверка конвертации поиска и замены ссылки баннера. тип {0}")
    public static Collection<Object[]> params() {
        Package bannerPackage = Banner.class.getPackage();
        Reflections reflections = new Reflections(bannerPackage.getName());
        List<Object[]> data = StreamEx.of(reflections.getSubTypesOf(Banner.class))
                .remove(cl -> Modifier.isAbstract(cl.getModifiers()))
                .remove(UNSUPPORTED_BANNER_CLASSES::contains)
                .map(b -> checkNotNull(BANNER_CLASS_TO_TYPE.get(b), "class %s must correspond to bannerType", b))
                .remove(UNSUPPORTED_BANNER_HREF_TYPES::contains)
                .distinct()
                .map(type -> new Object[]{type})
                .toList();
        checkState(!data.isEmpty(), "must have at least one banner type");
        return data;
    }

    @Test
    public void checkSupportedChangeHrefConverters() {
        GdiFindAndReplaceBannerHrefItem bannerHrefItem =
                new GdiFindAndReplaceBannerHrefItem().withBannerType(bannerType);
        Assertions.assertThatCode(() -> gdiBannerHrefToModelChanges(bannerHrefItem, new SitelinkSet().withId(1L), null))
                .doesNotThrowAnyException();
    }
}
