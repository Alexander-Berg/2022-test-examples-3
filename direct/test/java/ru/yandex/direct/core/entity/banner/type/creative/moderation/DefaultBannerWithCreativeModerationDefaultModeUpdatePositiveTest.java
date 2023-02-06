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
public class DefaultBannerWithCreativeModerationDefaultModeUpdatePositiveTest
        extends BannerWithCreativeModerationUpdatePositiveTestBase {

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление: баннер черновик, без изменений -> креатив остается в том же статусе",
                        // исходные значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        CpmIndoorBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        emptyModelChanges(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.YES,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.YES
                },
                {
                        "обновление: баннер черновик, изменился креатив -> креатив переходит в черновик",
                        // исходные значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        CpmIndoorBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewCreative(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.YES,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.NEW
                },
                {
                        "обновление: баннер активен, изменился креатив -> креатив отправлен на модерацию",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmIndoorBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewCreative(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.YES,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.READY
                },
        });
    }

    @Override
    protected AbstractBannerInfo<OldCpmIndoorBanner> createBanner() {
        var bannerCreativeInfo = steps.bannerCreativeSteps().createCpmIndoorBannerCreative(defaultClient);
        return bannerCreativeInfo.getBannerInfo();
    }

    private static BannerWithCreativeModelChangesFunction modelChangesWithNewCreative() {
        return (steps, clientInfo, bannerClass, bannerId) -> {
            Long creativeId = steps.creativeSteps()
                    .addDefaultCpmIndoorVideoCreative(clientInfo)
                    .getCreativeId();
            return getModelChanges(bannerClass, bannerId, creativeId);
        };
    }
}
