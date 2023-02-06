package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;

import static java.util.Arrays.asList;

@CoreTest
@RunWith(Parameterized.class)
public class DefaultBannerWithCreativeModerationForceSaveDraftUpdatePositiveTest
        extends BannerWithCreativeModerationUpdatePositiveTestBase {

    @Parameterized.Parameters(name = "mode {4}, {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление: баннер активен, без изменений -> креатив переводится в черновик",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmIndoorBanner.class,
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        //modelchange
                        emptyModelChanges(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.YES,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.NEW
                },
        });
    }

    @Override
    protected AbstractBannerInfo<OldCpmIndoorBanner> createBanner() {
        var bannerCreativeInfo = steps.bannerCreativeSteps().createCpmIndoorBannerCreative(defaultClient);
        return bannerCreativeInfo.getBannerInfo();
    }
}
