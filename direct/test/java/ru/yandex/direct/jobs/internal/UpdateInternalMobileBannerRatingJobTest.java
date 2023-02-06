package ru.yandex.direct.jobs.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.CoreMatchers;
import org.jooq.TransactionalCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.internal.UpdateInternalMobileBannerRatingJob.MobileTemplateVariablesHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.core.entity.internalads.Constants.MOBILE_APP_TEMPLATE_IDS;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.BANNER_1;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.BANNER_2;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.BANNER_3;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.PARSED_URL_1;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.PARSED_URL_2;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.SHARD;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.URL_1;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.URL_2;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.createBannerUrlCheckServiceMock;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.createDslContextProviderMock;
import static ru.yandex.direct.core.testing.mock.MobileBannerRatingMockUtils.createMobileContentServiceMock;

@ParametersAreNonnullByDefault
public class UpdateInternalMobileBannerRatingJobTest {

    @Mock
    private BannerTypedRepository bannerTypedRepository;

    private BannerUrlCheckService bannerUrlCheckService;

    private MobileContentService mobileContentService;

    private DslContextProvider ppcDslContextProvider;

    @Mock
    private BannerModifyRepository modifyRepository;

    private UpdateInternalMobileBannerRatingJob job;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);

        bannerUrlCheckService = createBannerUrlCheckServiceMock();

        mobileContentService = createMobileContentServiceMock();

        ppcDslContextProvider = createDslContextProviderMock();

        job = new UpdateInternalMobileBannerRatingJob(
                SHARD, bannerTypedRepository, bannerUrlCheckService, mobileContentService, ppcDslContextProvider,
                modifyRepository);
    }

    @Test
    void checkJob() {
        doReturn(List.of(BANNER_1, BANNER_2, BANNER_3)).when(bannerTypedRepository)
                .getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                        eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        job.execute();

        verify(bannerTypedRepository).getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        verify(bannerUrlCheckService).getRedirect(eq(URL_1));
        verify(bannerUrlCheckService).getRedirect(eq(URL_2));
        verifyNoMoreInteractions(bannerUrlCheckService);

        verify(mobileContentService).getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_1, PARSED_URL_2)));

        verify(ppcDslContextProvider).ppcTransactionResult(eq(SHARD), any(TransactionalCallable.class));

        verify(modifyRepository).updateByPredicate(any(), any(BannerRepositoryContainer.class),
                argThat(m -> m.size() == 2), eq(BannerWithInternalInfo.class), any(Predicate.class));
    }

    @Test
    void checkJob_withNoBanners() {
        doReturn(List.of()).when(bannerTypedRepository)
                .getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                        eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        job.execute();

        verify(bannerTypedRepository).getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        verifyZeroInteractions(bannerUrlCheckService);

        verifyZeroInteractions(mobileContentService);

        verifyZeroInteractions(ppcDslContextProvider);

        verifyZeroInteractions(modifyRepository);
    }

    @Test
    void checkJob_withNoNeedToUpdate() {
        doReturn(List.of(BANNER_3)).when(bannerTypedRepository)
                .getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                        eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        job.execute();

        verify(bannerTypedRepository).getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        verify(bannerUrlCheckService).getRedirect(eq(URL_1));
        verifyNoMoreInteractions(bannerUrlCheckService);

        verify(mobileContentService).getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_1)));

        verifyZeroInteractions(ppcDslContextProvider);

        verifyZeroInteractions(modifyRepository);
    }

    @Test
    void checkJob_withIncorrectRedirect() {
        doReturn(List.of(BANNER_1, BANNER_2, BANNER_3)).when(bannerTypedRepository)
                .getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                        eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        doReturn(RedirectCheckResult.createFailResult()).when(bannerUrlCheckService)
                .getRedirect(URL_1);

        job.execute();

        verify(bannerTypedRepository).getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        verify(bannerUrlCheckService).getRedirect(eq(URL_1));
        verify(bannerUrlCheckService).getRedirect(eq(URL_2));
        verifyNoMoreInteractions(bannerUrlCheckService);

        verify(mobileContentService).getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_2)));

        verify(ppcDslContextProvider).ppcTransactionResult(eq(SHARD), any(TransactionalCallable.class));

        verify(modifyRepository).updateByPredicate(any(), any(BannerRepositoryContainer.class),
                argThat(m -> m.size() == 1), eq(BannerWithInternalInfo.class), any(Predicate.class));
    }

    @Test
    void checkJob_withoutMobileContent() {
        doReturn(List.of(BANNER_1)).when(bannerTypedRepository)
                .getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                        eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        doReturn(Map.ofEntries(Map.entry(PARSED_URL_1, Optional.empty()))).when(mobileContentService)
                .getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_1)));

        job.execute();

        verify(bannerTypedRepository).getNoArchivedInternalBannersWithStatusShowYesByTemplateIds(
                eq(SHARD), eq(MOBILE_APP_TEMPLATE_IDS.keySet()));

        verify(bannerUrlCheckService).getRedirect(eq(URL_1));
        verifyNoMoreInteractions(bannerUrlCheckService);

        verify(mobileContentService).getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_1)));

        verifyZeroInteractions(ppcDslContextProvider);

        verifyZeroInteractions(modifyRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "https://redirect.appmetrica.yandex.com/serve/962757581464476054?text=vibrat_kvartiru&click_id={LOGID}&google_aid={GOOGLE_AID_LC}&ios_ifa={IDFA_UC}," +
                    "https://redirect.appmetrica.yandex.com/serve/962757581464476054," +
                    "true",
            "https://redirect.appmetrica.yandex.com/serve/962757581464476054," +
                    "https://redirect.appmetrica.yandex.com/serve/962757581464476054," +
                    "true",
            "https://redirect.appmetrica.yandex.com/serve/962757581464476054?text=vibrat_kvartiru&click_id={LOGID}&google_aid={GOOGLE_AID_LC}&ios_ifa={IDFA_UC}," +
                    "https://redirect.appmetrica.yandex.com/serve/962757581464476054?text=vibrat_kvartiru&click_id={LOGID}&google_aid={GOOGLE_AID_LC}&ios_ifa={IDFA_UC}," +
                    "false",
            "https://redirect.appmetrica.yandex.com/serve/962757581464476054?1?2," +
                    "https://redirect.appmetrica.yandex.com/serve/962757581464476054," +
                    "true"
    })
    void fromTemplateVariablesTest(String resourceUrl, String expectedUrl, boolean simplifyUrl) {
        List<TemplateVariable> templateVariables = List.of(
                new TemplateVariable().withTemplateResourceId(4059L).withInternalValue(resourceUrl),
                new TemplateVariable().withTemplateResourceId(4060L).withInternalValue("4.7"));

        MobileTemplateVariablesHolder variablesHolder = MobileTemplateVariablesHolder.fromTemplateVariables(
                templateVariables, MOBILE_APP_TEMPLATE_IDS.get(3103L), simplifyUrl);

        assertThat(variablesHolder.url, CoreMatchers.equalTo(expectedUrl));
    }
}
