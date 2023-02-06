package ru.yandex.direct.core.entity.banner.type.turbolanding.moderation;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class DefaultBannerWithTurboLandingModerationForceSaveDraftUpdatePositiveTest
        extends BannerWithTurboLandingModerationUpdatePositiveTestBase {

    @Parameterized.Parameters(name = "mode {4}, {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление: баннер активен, без изменений -> турболендинг переводится в черновик",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        TextBanner.class,
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        //modelchange
                        emptyModelChanges(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        BannerTurboLandingStatusModerate.NEW,
                        BannerStatusModerate.NEW
                },
        });
    }

    @Override
    protected TextBannerInfo createBanner() {
        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(defaultClient.getClientId());
        return steps.bannerSteps().createBanner(
                activeTextBanner(null, null)
                        .withTurboLandingId(turboLanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES), defaultClient);
    }
}
