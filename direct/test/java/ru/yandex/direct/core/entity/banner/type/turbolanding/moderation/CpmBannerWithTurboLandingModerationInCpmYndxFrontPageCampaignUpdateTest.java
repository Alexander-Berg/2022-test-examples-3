package ru.yandex.direct.core.entity.banner.type.turbolanding.moderation;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(Parameterized.class)
public class CpmBannerWithTurboLandingModerationInCpmYndxFrontPageCampaignUpdateTest
        extends BannerWithTurboLandingModerationUpdatePositiveTestBase {

    @Parameterized.Parameter(9)
    public BannerCreativeStatusModerate expectedBannerCreativeStatusModerate;

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление активного cpm баннера в cpm_yndx_frontPage кампании: изменился турболендинг " +
                                "-> турболендинг и баннер на модерации",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewTurboLanding(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        BannerTurboLandingStatusModerate.READY,
                        BannerStatusModerate.READY,
                        BannerCreativeStatusModerate.READY
                },
                {
                        "обновление активного cpm баннера в cpm_yndx_frontPage кампании: удаление турболендинга " +
                                "-> турболендинг удален и баннер на модерации",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithDeleteTurboLanding(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        null,
                        BannerStatusModerate.READY,
                        BannerCreativeStatusModerate.READY
                },
        });
    }

    @Override
    protected CpmBannerInfo createBanner() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(defaultClient.getClientId());
        steps.creativeSteps().addDefaultHtml5CreativeForFrontpage(defaultClient, creativeId);

        return steps.bannerSteps().createActiveCpmYndxFrontpageBanner(
                activeCpmBanner(null, null, creativeId)
                        .withTurboLandingId(turboLanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES),
                defaultClient);
    }
}
