package ru.yandex.direct.core.entity.banner.type.sitelink;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithSitelinks;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithSitelinks;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithSitelinksUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithSitelinks> {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public TestModerationRepository testModerationRepository;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerProvider bannerProvider;

    @Parameterized.Parameter(2)
    public BannerModelChangesProvider modelChangesProvider;

    @Parameterized.Parameter(3)
    public ExpectedSitelinkSetIdProvider expectedSitelinksSetIdProvider;

    @Parameterized.Parameter(4)
    public StatusBsSynced expectedStatusBsSynced;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "баннер с сайтлинками, нет изменений",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        emptyModelChanges(),
                        // ожидаемые значения
                        sitelinksFromOriginalBanner(),
                        StatusBsSynced.YES
                },
                {
                        "баннер без сайтлинков, нет изменений",
                        // исходный баннер
                        bannerWithoutSitelinks(activeTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        emptyModelChanges(),
                        // ожидаемые значения
                        sitelinksNull(),
                        StatusBsSynced.YES
                },
                {
                        "добавили сайтлинки",
                        // исходный баннер
                        bannerWithoutSitelinks(activeTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        newSitelinksModelChanges(),
                        // ожидаемые значения
                        sitelinksFromModelChanges(),
                        StatusBsSynced.NO
                },
                {
                        "удалили сайтлинки",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        deleteSitelinksModelChanges(),
                        // ожидаемые значения
                        sitelinksNull(),
                        StatusBsSynced.NO
                },
                {
                        "изменили сайтлинки",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        newSitelinksModelChanges(),
                        // ожидаемые значения
                        sitelinksFromModelChanges(),
                        StatusBsSynced.NO
                },
        });
    }

    @Test
    public void test() {
        bannerInfo = bannerProvider.createBanner(steps);

        ModelChanges<BannerWithSystemFields> modelChanges =
                modelChangesProvider.getModelChanges(steps, bannerInfo);
        prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(bannerInfo.getBannerId());
        Long expectedSitelinksSetId =
                expectedSitelinksSetIdProvider.getExpectedSitelinksSetId(bannerInfo, modelChanges);
        assertThat(actualBanner.getSitelinksSetId()).isEqualTo(expectedSitelinksSetId);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
    }

    protected static BannerProvider bannerWithoutSitelinks(OldTextBanner banner) {
        return steps -> steps.bannerSteps().createBanner(banner);
    }

    protected static BannerProvider bannerWithSitelinks(OldTextBanner banner) {
        return steps -> {
            SitelinkSetInfo sitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet();
            ClientInfo client = sitelinkSet.getClientInfo();
            return steps.bannerSteps().createBanner(banner.withSitelinksSetId(sitelinkSet.getSitelinkSetId()), client);
        };
    }

    private static BannerModelChangesProvider emptyModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static BannerModelChangesProvider newSitelinksModelChanges() {
        return (steps, banner) -> {
            SitelinkSetInfo newSitelinkSet =
                    steps.sitelinkSetSteps().createDefaultSitelinkSet(banner.getClientInfo());
            return new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                    .process(newSitelinkSet.getSitelinkSetId(), BannerWithSitelinks.SITELINKS_SET_ID)
                    .castModelUp(BannerWithSystemFields.class);
        };
    }

    private static BannerModelChangesProvider deleteSitelinksModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .process(null, BannerWithSitelinks.SITELINKS_SET_ID)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static ExpectedSitelinkSetIdProvider sitelinksFromOriginalBanner() {
        return (originalBanner, modelChanges) -> originalBanner.getBanner().getSitelinksSetId();
    }

    private static ExpectedSitelinkSetIdProvider sitelinksFromModelChanges() {
        return (originalBanner, modelChanges) -> modelChanges
                .castModelUp(BannerWithSitelinks.class)
                .getChangedProp(BannerWithSitelinks.SITELINKS_SET_ID);
    }

    private static ExpectedSitelinkSetIdProvider sitelinksNull() {
        return (originalBanner, modelChanges) -> null;
    }

    interface BannerProvider {
        TextBannerInfo createBanner(Steps steps);
    }

    interface BannerModelChangesProvider {
        ModelChanges<BannerWithSystemFields> getModelChanges(Steps steps,
                                                             AbstractBannerInfo<? extends OldBannerWithSitelinks> banner);
    }

    interface ExpectedSitelinkSetIdProvider {
        Long getExpectedSitelinksSetId(AbstractBannerInfo<? extends OldBannerWithSitelinks> originalBanner,
                                       ModelChanges<BannerWithSystemFields> modelChanges);
    }
}
