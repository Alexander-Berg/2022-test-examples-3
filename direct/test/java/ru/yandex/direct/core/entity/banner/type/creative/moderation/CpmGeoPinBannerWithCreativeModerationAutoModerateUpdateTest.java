package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CpmGeoPinBannerInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.common.db.PpcPropertyNames.CPM_GEO_PIN_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmGeoPinBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;

@CoreTest
@RunWith(Parameterized.class)
public class CpmGeoPinBannerWithCreativeModerationAutoModerateUpdateTest
        extends BannerWithCreativeModerationUpdatePositiveTestBase {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление активного баннера с геопином: изменился креатив -> креатив автоматически " +
                                "модерируется",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmGeoPinBanner.class,
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
                        "обновление активного баннера с геопином: креатив на модерации, изменился креатив " +
                                "-> креатив автоматически модерируется",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        CpmGeoPinBanner.class,
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

    @Before
    public void before() throws Exception {
        super.before();
        ppcPropertiesSupport.set(CPM_GEO_PIN_AUTO_MODERATION.getName(), "true");
    }

    @After
    public void after() {
        ppcPropertiesSupport.remove(CPM_GEO_PIN_AUTO_MODERATION.getName());
    }

    @Override
    protected CpmGeoPinBannerInfo createBanner() {
        var permalinkIdFirst = defaultActiveOrganization(defaultClient.getClientId()).getPermalinkId();

        var creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCanvasCreative(defaultClient, creativeId);

        var banner = activeCpmGeoPinBanner(null, null, creativeId, permalinkIdFirst);
        return steps.bannerSteps().createActiveCpmGeoPinBanner(banner, defaultClient);
    }

    private static BannerWithCreativeModelChangesFunction modelChangesWithNewCreative() {
        return (steps, clientInfo, bannerClass, bannerId) -> {
            Long creativeId = steps.creativeSteps().getNextCreativeId();
            steps.creativeSteps().addDefaultCanvasCreative(clientInfo, creativeId);
            return getModelChanges(bannerClass, bannerId, creativeId);
        };
    }
}
