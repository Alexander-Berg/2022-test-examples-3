package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.Collection;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.common.db.PpcPropertyNames.CPM_GEOPRODUCT_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBannerWithTurbolanding;

@CoreTest
@RunWith(Parameterized.class)
public class CpmGeoProductBannerWithCreativeModerationNoAutoModerateUpdateTest
        extends BannerWithCreativeModerationUpdatePositiveTestBase {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление активного баннера с геопродуктом: изменился креатив -> креатив на " +
                                "модерации",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmBanner.class,
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

    @Before
    public void before() throws Exception {
        super.before();
        ppcPropertiesSupport.remove(CPM_GEOPRODUCT_AUTO_MODERATION.getName());
    }

    @Override
    protected CpmBannerInfo createBanner() {
        var turbolandingId = steps.turboLandingSteps()
                .createDefaultBannerTurboLanding(defaultClient.getClientId()).getId();
        Long creativeId = steps.creativeSteps()
                .addDefaultHtml5CreativeForGeoproduct(defaultClient).getCreativeId();
        OldCpmBanner banner = activeCpmBannerWithTurbolanding(null, null, creativeId, turbolandingId);
        return steps.bannerSteps().createActiveCpmGeoproductBanner(banner, defaultClient);
    }

    private static BannerWithCreativeModelChangesFunction modelChangesWithNewCreative() {
        return (steps, clientInfo, bannerClass, bannerId) -> {
            Long creativeId = steps.creativeSteps()
                    .addDefaultHtml5CreativeForGeoproduct(clientInfo).getCreativeId();
            return getModelChanges(bannerClass, bannerId, creativeId);
        };
    }
}
