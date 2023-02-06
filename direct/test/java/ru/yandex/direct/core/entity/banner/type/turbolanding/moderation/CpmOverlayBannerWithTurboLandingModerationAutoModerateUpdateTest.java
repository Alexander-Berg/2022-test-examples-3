package ru.yandex.direct.core.entity.banner.type.turbolanding.moderation;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;

@CoreTest
@RunWith(Parameterized.class)
public class CpmOverlayBannerWithTurboLandingModerationAutoModerateUpdateTest
        extends BannerWithTurboLandingModerationUpdatePositiveTestBase {

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление оверлейного активного баннера: изменился турболендинг " +
                                "-> турболендинг автоматически модерируется",
                        // исходные значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NEW,
                        CpmBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewTurboLanding(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        BannerTurboLandingStatusModerate.YES,
                        BannerStatusModerate.YES,
                },
                {
                        "обновление оверлейного активного баннера: турболендинг на модерации, изменился турболендинг " +
                                "-> турболендинг модерируется",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewTurboLanding(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.READY,
                        // ожидаемые значения
                        BannerTurboLandingStatusModerate.YES,
                        BannerStatusModerate.YES,
                },
        });
    }

    @Override
    protected CpmBannerInfo createBanner() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(defaultClient.getClientId());
        steps.creativeSteps().addDefaultOverlayCreative(defaultClient, creativeId);

        return steps.bannerSteps().createActiveCpmVideoBanner(
                activeCpmVideoBanner(null, null, creativeId)
                        .withTurboLandingId(turboLanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES),
                defaultClient);
    }
}
