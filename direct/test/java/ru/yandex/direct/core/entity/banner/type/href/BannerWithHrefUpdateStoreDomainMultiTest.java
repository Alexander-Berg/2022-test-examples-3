package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewCpcVideoBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithHrefUpdateStoreDomainMultiTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    @Autowired
    private TestNewCpcVideoBanners testNewCpcVideoBanners;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String initialHref1;

    @Parameterized.Parameter(2)
    public String newHref1;

    @Parameterized.Parameter(3)
    public String newDomain1;

    @Parameterized.Parameter(4)
    public String initialHref2;

    @Parameterized.Parameter(5)
    public String newHref2;

    @Parameterized.Parameter(6)
    public String newDomain2;

    private NewCpcVideoBannerInfo bannerInfo2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "href1: null -> null; href2: null -> yandex.ru",
                        // 1 banner
                        null,
                        null,
                        null,
                        // 2 banner
                        null,
                        "https://www.yandex.ru",
                        "www.yandex.ru"
                },
                {
                        "href1: null -> habr.com; href2: null -> yandex.ru",
                        // 1 banner
                        null,
                        "https://habr.com",
                        "habr.com",
                        // 2 banner
                        null,
                        "https://www.yandex.ru",
                        "www.yandex.ru"
                },
                {
                        "href1: habr.com -> null; href2: yandex.ru -> null",
                        // 1 banner
                        "https://habr.com",
                        null,
                        null,
                        // 2 banner
                        "https://www.yandex.ru",
                        null,
                        null,
                },
                {
                        "href1: habr.com -> www.instagram.com; href2: yandex.ru -> null",
                        // 1 banner
                        "https://habr.com",
                        "https://www.instagram.com",
                        "www.instagram.com",
                        // 2 banner
                        "https://www.yandex.ru",
                        null,
                        null,
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        TurboLanding turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());

        CpcVideoBanner initialBanner1 = testNewCpcVideoBanners.fullCpcVideoBanner(null)
                .withHref(initialHref1)
                .withDomain(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(initialHref1))
                .withTurboLandingId(turboLanding.getId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES);
        CpcVideoBanner initialBanner2 = testNewCpcVideoBanners.fullCpcVideoBanner(null)
                .withHref(initialHref2)
                .withDomain(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(initialHref2))
                .withTurboLandingId(turboLanding.getId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES);

        bannerInfo = steps.cpcVideoBannerSteps()
                .createBanner(new NewCpcVideoBannerInfo()
                        .withClientInfo(clientInfo)
                        .withBanner(initialBanner1));

        bannerInfo2 = steps.cpcVideoBannerSteps()
                .createBanner(new NewCpcVideoBannerInfo()
                        .withClientInfo(clientInfo)
                        .withBanner(initialBanner2));
    }

    @Test
    public void domainIsStoredOnHrefUpdate() {
        ModelChanges<CpcVideoBanner> modelChanges1 =
                ModelChanges.build(bannerInfo.getBannerId(), CpcVideoBanner.class,
                        CpcVideoBanner.HREF, newHref1);
        ModelChanges<CpcVideoBanner> modelChanges2 =
                ModelChanges.build(bannerInfo2.getBannerId(), CpcVideoBanner.class,
                        CpcVideoBanner.HREF, newHref2);
        prepareAndApplyValid(asList(modelChanges1, modelChanges2));

        checkBanner(bannerInfo.getBannerId(), newHref1, newDomain1);
        checkBanner(bannerInfo2.getBannerId(), newHref2, newDomain2);
    }

    private void checkBanner(Long bannerId, String newHref, String newDomain) {
        CpcVideoBanner actualBanner = getBanner(bannerId);

        if (newHref == null) {
            assertThat(actualBanner.getDomain()).isNull();
            assertThat(actualBanner.getDomainId()).isNull();
            return;
        }

        assertThat(actualBanner.getDomain()).isEqualTo(newDomain);

        List<Domain> domains = domainRepository.getDomains(bannerInfo.getShard(),
                singletonList(actualBanner.getDomain()));
        assertThat(domains).hasSize(1);
        assertThat(actualBanner.getDomainId()).isEqualTo(domains.get(0).getId());
    }
}
