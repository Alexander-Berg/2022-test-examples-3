package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;

@CoreTest
@RunWith(Parameterized.class)
public class CpmOverlayBannerWithCreativeModerationAutoModerateUpdateTest
        extends BannerWithCreativeModerationUpdatePositiveTestBase {

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление оверлейного активного баннера: изменился креатив -> креатив автоматически " +
                                "модерируется",
                        // исходные значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NEW,
                        CpmBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewCreative(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.YES,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.YES
                },
                {
                        "обновление оверлейного активного баннера: креатив на модерации, изменился креатив " +
                                "-> креатив автоматически модерируется",
                        // исходные значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NEW,
                        CpmBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewCreative(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.READY,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.YES
                },
        });
    }

    @Override
    protected CpmBannerInfo createBanner() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultOverlayCreative(defaultClient, creativeId);

        return steps.bannerSteps().createActiveCpmVideoBanner(
                activeCpmVideoBanner(null, null, creativeId),
                defaultClient);
    }

    private static BannerWithCreativeModelChangesFunction modelChangesWithNewCreative() {
        return (steps, clientInfo, bannerClass, bannerId) -> {
            Long creativeId = steps.creativeSteps().getNextCreativeId();
            steps.creativeSteps().addDefaultOverlayCreative(clientInfo, creativeId);
            return getModelChanges(bannerClass, bannerId, creativeId);
        };
    }
}
