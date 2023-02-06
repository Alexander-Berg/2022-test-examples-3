package ru.yandex.direct.core.entity.moderation.service.receiving.operations.common;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.domain.model.ApiDomainStat;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.moderation.repository.bulk_update.BulkUpdateHolder;
import ru.yandex.direct.core.entity.moderation.service.receiving.operations.banners.BannersUpdateApiDomainStatOp;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeTextBannerResponse;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.DIAG_ID1;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.DIAG_ID2;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdateApiDomainStatOpTest {
    private static final long CRITICAL_REASON = DIAG_ID1;
    private static final long NON_CRITICAL_REASON = DIAG_ID2;
    private static final long UNKNOWN_REASON = 100500L;

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private ModerationDiagService moderationDiagService;
    @Autowired
    private Steps steps;

    private final BulkUpdateHolder bulkUpdateHolder = new BulkUpdateHolder();

    @Autowired
    private BannersUpdateApiDomainStatOp updateApiDomainStat;

    private LocalDate today;
    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        moderationDiagService.invalidateAll();
        steps.moderationDiagSteps().insertStandartDiags();
        clientInfo = steps.clientSteps().createDefaultClient();

        today = LocalDate.now();
    }

    @Test
    public void objectAccepted() {
        var domain = newDomain();
        var bid = createBannerWithDomain(domain);

        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid, 42L, Yes, List.of()));
        updateApiDomainStat.flush(dslContextProvider.ppc(clientInfo.getShard()).configuration(), bulkUpdateHolder);

        var expectedStat = apiDomainStat(domain).withAcceptedItems(1L);
        var stat = domainRepository.getDomainsStat(List.of(domain));
        assertThat(stat, contains(beanDiffer(expectedStat)));
    }

    @Test
    public void objectDeclined() {
        var domain = newDomain();
        var bid = createBannerWithDomain(domain);

        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid, 42L, No, List.of(NON_CRITICAL_REASON)));
        updateApiDomainStat.flush(dslContextProvider.ppc(clientInfo.getShard()).configuration(), bulkUpdateHolder);

        var expectedStat = apiDomainStat(domain).withDeclinedItems(1L);
        var stat = domainRepository.getDomainsStat(List.of(domain));
        assertThat(stat, contains(beanDiffer(expectedStat)));
    }

    @Test
    public void objectDeclinedWithBadReason() {
        var domain = newDomain();
        var bid = createBannerWithDomain(domain);

        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid, 42L, No, List.of(CRITICAL_REASON,
                NON_CRITICAL_REASON)));
        updateApiDomainStat.flush(dslContextProvider.ppc(clientInfo.getShard()).configuration(), bulkUpdateHolder);

        var expectedStat = apiDomainStat(domain).withDeclinedItems(1L).withBadReasons(1L);
        var stat = domainRepository.getDomainsStat(List.of(domain));
        assertThat(stat, contains(beanDiffer(expectedStat)));
    }

    @Test
    public void objectDeclinedWithUnknownReason() {
        var domain = newDomain();
        var bid = createBannerWithDomain(domain);

        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid, 42L, No, List.of(UNKNOWN_REASON,
                CRITICAL_REASON)));
        updateApiDomainStat.flush(dslContextProvider.ppc(clientInfo.getShard()).configuration(), bulkUpdateHolder);


        var expectedStat = apiDomainStat(domain).withDeclinedItems(1L).withBadReasons(2L);
        var stat = domainRepository.getDomainsStat(List.of(domain));
        assertThat(stat, contains(beanDiffer(expectedStat)));
    }

    @Test
    public void multipleObjects() {
        var domain1 = newDomain();
        var domain2 = newDomain();
        var bid1 = createBannerWithDomain(domain1);
        var bid2 = createBannerWithDomain(domain1);
        var bid3 = createBannerWithDomain(domain2);
        var bid4 = createBannerWithDomain(domain2);


        // первый баннер был промодерирован дважды
        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid1, 42L, Yes, List.of()));
        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid1, 43L, No,
                List.of(NON_CRITICAL_REASON)));
        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid2, 42L, Yes, List.of()));

        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid3, 42L, No, List.of(CRITICAL_REASON)));
        updateApiDomainStat.consume(bulkUpdateHolder, makeTextBannerResponse(bid4, 42L, Yes, List.of()));

        updateApiDomainStat.flush(dslContextProvider.ppc(clientInfo.getShard()).configuration(), bulkUpdateHolder);

        // первый баннер два раза посчитали, это ок
        var expectedStat1 = apiDomainStat(domain1).withAcceptedItems(2L).withDeclinedItems(1L).withBadReasons(0L);
        var expectedStat2 = apiDomainStat(domain2).withAcceptedItems(1L).withDeclinedItems(1L).withBadReasons(1L);
        var stat = domainRepository.getDomainsStat(List.of(domain1, domain2));

        assertThat(stat, containsInAnyOrder(beanDiffer(expectedStat1), beanDiffer(expectedStat2)));
    }

    private String newDomain() {
        return RandomNumberUtils.nextPositiveInteger() + ".testdomain.com";
    }

    private Long createBannerWithDomain(String domain) {
        return steps.bannerSteps()
                .createBanner(activeTextBanner().withDomain(domain), clientInfo)
                .getBannerId();
    }

    private ApiDomainStat apiDomainStat(String domain) {
        return new ApiDomainStat().withFilterDomain(domain).withStatDate(today)
                .withAcceptedItems(0L).withDeclinedItems(0L).withBadReasons(0L);
    }
}
