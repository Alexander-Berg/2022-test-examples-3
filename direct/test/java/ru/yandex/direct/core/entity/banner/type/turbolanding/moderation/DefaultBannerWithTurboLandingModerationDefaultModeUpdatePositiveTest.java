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
public class DefaultBannerWithTurboLandingModerationDefaultModeUpdatePositiveTest
        extends BannerWithTurboLandingModerationUpdatePositiveTestBase {

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление: баннер черновик, без изменений -> турболендинг остается в том же статусе",
                        // исходные значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        TextBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        emptyModelChanges(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        BannerTurboLandingStatusModerate.YES,
                        BannerStatusModerate.NEW
                },
                {
                        "обновление: баннер черновик, изменился турболендинг -> турболендинг переходит в черновик",
                        // исходные значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        TextBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewTurboLanding(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        BannerTurboLandingStatusModerate.NEW,
                        BannerStatusModerate.NEW
                },
                {
                        "обновление: баннер активен, изменился турболендинг -> турболендинг отправлен на модерацию",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        TextBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewTurboLanding(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        BannerTurboLandingStatusModerate.READY,
                        BannerStatusModerate.YES
                },
                {
                        "обновление: баннер активен, удалить турболендинг -> турболендинг отправлен на модерацию",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        TextBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithDeleteTurboLanding(),
                        //исходный статус связанного объекта
                        BannerTurboLandingStatusModerate.YES,
                        // ожидаемые значения
                        null,
                        BannerStatusModerate.YES
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
