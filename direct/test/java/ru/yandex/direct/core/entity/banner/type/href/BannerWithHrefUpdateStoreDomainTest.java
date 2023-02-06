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
public class BannerWithHrefUpdateStoreDomainTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    @Autowired
    private TestNewCpcVideoBanners testNewCpcVideoBanners;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String initialHref;

    @Parameterized.Parameter(2)
    public String newHref;

    @Parameterized.Parameter(3)
    public String newDomain;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "href: null -> null",
                        null,
                        null,
                        null
                },
                {
                        "href: null -> habr.com",
                        null,
                        "https://habr.com",
                        "habr.com"
                },
                {
                        "href: www.yandex.ru -> habr.com",
                        "https://www.yandex.ru",
                        "https://habr.com",
                        "habr.com"
                },
                {
                        "href: www.yandex.ru -> null",
                        "https://www.yandex.ru",
                        null,
                        null
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        TurboLanding turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());
        CpcVideoBanner initialBanner = testNewCpcVideoBanners.fullCpcVideoBanner(null)
                .withHref(initialHref)
                .withDomain(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(initialHref))
                .withTurboLandingId(turboLanding.getId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES);

        bannerInfo = steps.cpcVideoBannerSteps()
                .createBanner(new NewCpcVideoBannerInfo()
                        .withClientInfo(clientInfo)
                        .withBanner(initialBanner));
    }

    @Test
    public void domainIsStoredOnHrefUpdate() {
        ModelChanges<CpcVideoBanner> modelChanges =
                ModelChanges.build(bannerInfo.getBannerId(), CpcVideoBanner.class,
                        CpcVideoBanner.HREF, newHref);
        prepareAndApplyValid(modelChanges);

        CpcVideoBanner actualBanner = getBanner(bannerInfo.getBannerId());

        if (newHref == null) {
            assertThat(actualBanner.getDomain()).isNull();
            assertThat(actualBanner.getDomainId()).isNull();
            return;
        }

        assertThat(actualBanner.getDomain()).isEqualTo(newDomain);

        List<Domain> domains = domainRepository.getDomains(bannerInfo.getShard(), singletonList(actualBanner.getDomain()));
        assertThat(domains).hasSize(1);
        assertThat(domains.get(0).getId()).isEqualTo(actualBanner.getDomainId());
    }
}
