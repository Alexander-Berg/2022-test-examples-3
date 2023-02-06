package ru.yandex.direct.core.entity.banner.type.href;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.banner.model.BannerWithHref.DOMAIN;
import static ru.yandex.direct.core.entity.banner.type.Helpers.createAppliedChanges;
import static ru.yandex.direct.core.entity.banner.type.Helpers.createModelChanges;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerHrefAndDomainProcessorTest {
    private static final String TRUSTED_DOMAIN = "ya.ru";

    @Autowired
    private DomainService domainService;
    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private BannerCommonRepository bannerRepository;
    private BannerCommonRepository bannerRepositorySpy;
    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    @Mock
    private TrustedRedirectsService trustedRedirectService;

    private BannerHrefAndDomainProcessor hrefProcessor;

    @Before
    public void setUp() {
        initMocks(this);
        when(trustedRedirectService.isDomainTrusted(endsWith(TRUSTED_DOMAIN))).thenReturn(true);
        when(trustedRedirectService.isDomainTrusted(not(endsWith(TRUSTED_DOMAIN)))).thenReturn(false);
        bannerRepositorySpy = spy(bannerRepository);
        hrefProcessor = new BannerHrefAndDomainProcessor(domainService, bannerRepositorySpy, trustedRedirectService,
                adGroupRepository, bannersUrlHelper);
    }

    @Test
    public void addBannersToRedirectCheckQueue() {
        var banners = asList(
                createNewBannerWithHref().withDomain("yandex.ru").withId(nextPositiveLong()),
                createNewBannerWithHref().withDomain(TRUSTED_DOMAIN).withId(nextPositiveLong())
        );

        hrefProcessor.addBannersToRedirectCheckQueue(DEFAULT_SHARD, banners);

        ArgumentCaptor<List<Long>> bannersSentToRedirectCheckCaptor = ArgumentCaptor.forClass(List.class);
        verify(bannerRepositorySpy).addToRedirectCheckQueue(
                eq(DEFAULT_SHARD), bannersSentToRedirectCheckCaptor.capture());
        assertThat(bannersSentToRedirectCheckCaptor.getValue(), contains(banners.get(1).getId()));
    }

    @Test
    public void addBannersChangesToRedirectCheckQueue() {
        var banners = List.of(
                createNewBannerWithHref().withDomain("yandex.ru").withId(nextPositiveLong()),
                createNewBannerWithHref().withDomain("yandex.ru").withId(nextPositiveLong())
        );
        var changes = createModelChanges(banners);
        changes.get(0).process(TRUSTED_DOMAIN, DOMAIN);
        changes.get(1).process("untrusted.com", DOMAIN);

        hrefProcessor.addBannersChangesToRedirectCheckQueue(DEFAULT_SHARD, createAppliedChanges(banners, changes));

        ArgumentCaptor<List<Long>> bannersSentToRedirectCheckCaptor = ArgumentCaptor.forClass(List.class);
        verify(bannerRepositorySpy).addToRedirectCheckQueue(eq(DEFAULT_SHARD),
                bannersSentToRedirectCheckCaptor.capture());
        assertThat(bannersSentToRedirectCheckCaptor.getValue(), contains(banners.get(0).getId()));
    }

    private static BannerWithHref createNewBannerWithHref() {
        return new ContentPromotionBanner().withHref("https://ya.ru");
    }
}
