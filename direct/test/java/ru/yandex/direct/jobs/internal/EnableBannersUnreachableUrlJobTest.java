package ru.yandex.direct.jobs.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.internalads.model.InternalTemplateInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceType;
import ru.yandex.direct.core.entity.internalads.service.TemplateInfoService;
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.internal.model.StructureOfBannerIds;
import ru.yandex.direct.jobs.internal.utils.InfoForUrlNotificationsGetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@ParametersAreNonnullByDefault
public class EnableBannersUnreachableUrlJobTest {
    private static final int JOB_SHARD = 10;

    @Mock
    private BannerTypedRepository bannerTypedRepository;

    @Mock
    private DslContextProvider ppcDslContextProvider;

    @Mock
    private BannerModifyRepository modifyRepository;

    @Mock
    private BannerUrlCheckService bannerUrlCheckService;

    @Mock
    private UrlMonitoringNotifyService urlMonitoringNotifyService;

    @Mock
    private TemplateInfoService templateInfoService;

    @Mock
    private InfoForUrlNotificationsGetter infoForUrlNotificationsGetter;

    @Spy
    @InjectMocks
    private EnableBannersUnreachableUrlJob job;

    @Captor
    private ArgumentCaptor<Map<Long, Set<String>>> urlsByBannerIdCaptor;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);

        doReturn(JOB_SHARD)
                .when(job).getShard();

        doReturn(bannersTestData())
                .when(bannerTypedRepository).getNoArchivedInternalBannersStoppedByUrlMonitoring(JOB_SHARD);

        doReturn(getByTemplateIdsTestData())
                .when(templateInfoService).getByTemplateIds(getTemplateIdsTestData());

        doCallRealMethod().when(infoForUrlNotificationsGetter).getAdditionalInfoByInternalBanners(eq(JOB_SHARD),
                anyList());

        doAnswer(invocation -> {
            Map<Long, CampaignSimple> campaigns = campaignsTestData();
            List<InternalBanner> banners = invocation.getArgument(1);
            Set<Long> targetCampaignIds = StreamEx.of(banners).map(InternalBanner::getCampaignId).toSet();
            return EntryStream.of(campaigns).filterKeys(targetCampaignIds::contains).mapValues(CampaignSimple::getName).toMap();
        }).when(infoForUrlNotificationsGetter).getCampaignNamesFromItsBanners(eq(JOB_SHARD), anyList());

        for (var urlAndCheckResult : getUrlCheckResultMapTestData().entrySet()) {
            doReturn(urlAndCheckResult.getValue())
                    .when(bannerUrlCheckService).isUrlReachable(urlAndCheckResult.getKey());
        }
    }

    @Test
    void checkJob() {
        job.execute();

        verify(bannerTypedRepository).getNoArchivedInternalBannersStoppedByUrlMonitoring(JOB_SHARD);

        verify(job).getUrlsByBannerId(anyList());
        verify(job).getBannersToEnable(anyMap());
        verify(job).enableBanners(anyList());
        verify(job).getStructuredAndFilteredBannerIds(anyList(), any());

        verify(urlMonitoringNotifyService).notifyBannersEnabled(anyList());
    }

    @Test
    void checkGetUrlsByBannerId() {
        job.execute();

        verify(job).getBannersToEnable(urlsByBannerIdCaptor.capture());

        Map<Long, Set<String>> expected = Map.of(
                10L, Set.of("correct url"),
                11L, Set.of("wrong url"),
                12L, Set.of("ok url", "again ok url"),
                13L, Set.of("bad url", "ok url")
        );

        assertThat(urlsByBannerIdCaptor.getValue())
                .isEqualTo(expected);
    }

    @Test
    void checkGetUrlResourceIdsByTemplateId() {
        Map<Long, Set<Long>> urlResourceIdsByTemplateId = job.getUrlResourceIdsByTemplateId(bannersTestData());

        Map<Long, Set<Long>> expected = Map.of(
                1L, Set.of(1L),
                2L, Set.of(3L, 4L)
        );

        assertThat(urlResourceIdsByTemplateId)
                .isEqualTo(expected);
    }

    @Test
    void checkExtractUrls() {
        InternalBanner banner = createBanner(12L, 1L, 1L, List.of(
                createTemplateVariable(1L, "url"),
                createTemplateVariable(2L, "not url"),
                createTemplateVariable(3L, "not url again"),
                createTemplateVariable(100L, "url!")
        ));
        Set<Long> urlResourceIds = Set.of(1L, 100L);

        Set<String> urls = job.extractUrls(banner, urlResourceIds);

        Set<String> expected = Set.of("url", "url!");

        assertThat(urls)
                .isEqualTo(expected);
    }

    @Test
    void checkGetBannersToEnable() {
        job.execute();

        verify(job).getBannersToEnable(urlsByBannerIdCaptor.capture());

        List<Long> bannerIds = job.getBannersToEnable(urlsByBannerIdCaptor.getValue());

        List<Long> expected = List.of(10L, 12L);

        assertThat(bannerIds)
                .isEqualTo(expected);
    }

    @Test
    void checkGetStructuredAndFilteredBannerIds() {
        List<InternalBanner> banners = List.of(
                createBanner(1L, 11L, 2L, List.of(createTemplateVariable(1L, "value"))),
                createBanner(11L, 11L, 2L, List.of(createTemplateVariable(1L, "value"))),
                createBanner(2L, 11L, 2L, List.of(createTemplateVariable(1L, "v"))),
                createBanner(3L, 22L, 5L, List.of(createTemplateVariable(1L, "v1"),
                        createTemplateVariable(2L, "v2"))
                )
        );
        Predicate<InternalBanner> filter = banner -> List.of(1L, 11L, 3L).contains(banner.getId());

        List<StructureOfBannerIds> containers = job.getStructuredAndFilteredBannerIds(banners, filter);

        List<StructureOfBannerIds> expected = List.of(
                new StructureOfBannerIds(11, "camp11", Map.of(2L, List.of(1L, 11L))),
                new StructureOfBannerIds(22, "camp22", Map.of(5L, List.of(3L)))
        );

        assertThat(containers).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static List<InternalBanner> bannersTestData() {
        return List.of(createBanner(10L, 11L, 1L, List.of(
                        createTemplateVariable(1L, "correct url"),
                        createTemplateVariable(2L, "image.png"))
                ),
                createBanner(11L, 11L, 1L, List.of(
                        createTemplateVariable(1L, "wrong url"),
                        createTemplateVariable(2L, "image.png"))
                ),
                createBanner(12L, 22L, 2L, List.of(
                        createTemplateVariable(3L, "ok url"),
                        createTemplateVariable(4L, "again ok url"))
                ),
                createBanner(13L, 33L, 2L, List.of(
                        createTemplateVariable(3L, "bad url"),
                        createTemplateVariable(4L, "ok url"))
                ),
                createBanner(14L, 33L, 100L, List.of(
                        createTemplateVariable(5L, "18"),
                        createTemplateVariable(6L, "text"))
                )
        );
    }

    private static Map<Long, CampaignSimple> campaignsTestData() {
        return StreamEx.of(List.of(
                createCampaign(11L, "camp11"),
                createCampaign(22L, "camp22"),
                createCampaign(33L, "camp33")
        )).toMap(CampaignSimple::getId, Function.identity());
    }

    private static Set<Long> getTemplateIdsTestData() {
        return listToSet(bannersTestData(), InternalBanner::getTemplateId);
    }

    private static List<InternalTemplateInfo> getByTemplateIdsTestData() {
        return List.of(createTemplateInfo(1L, List.of(
                        createResourceInfo(1L, ResourceType.URL),
                        createResourceInfo(2L, ResourceType.IMAGE))
                ),
                createTemplateInfo(2L, List.of(
                        createResourceInfo(3L, ResourceType.URL),
                        createResourceInfo(4L, ResourceType.URL))
                ),
                createTemplateInfo(100L, List.of(
                        createResourceInfo(5L, ResourceType.AGE),
                        createResourceInfo(6L, ResourceType.TEXT))
                )
        );
    }

    private static Map<String, UrlCheckResult> getUrlCheckResultMapTestData() {
        return Map.of(
                "correct url", new UrlCheckResult(true, null),
                "wrong url", new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT),
                "ok url", new UrlCheckResult(true, null),
                "again ok url", new UrlCheckResult(true, null),
                "bad url", new UrlCheckResult(false, UrlCheckResult.Error.HTTP_ERROR)
        );
    }

    private static InternalBanner createBanner(Long id, Long campaignId, Long templateId,
                                               List<TemplateVariable> templateVariables) {
        return new InternalBanner()
                .withId(id)
                .withCampaignId(campaignId)
                .withTemplateId(templateId)
                .withTemplateVariables(templateVariables);
    }

    private static CampaignSimple createCampaign(Long id, String name) {
        return new Campaign()
                .withId(id)
                .withName(name);
    }

    private static TemplateVariable createTemplateVariable(Long resourceId, String internalValue) {
        return new TemplateVariable()
                .withTemplateResourceId(resourceId)
                .withInternalValue(internalValue);
    }

    private static InternalTemplateInfo createTemplateInfo(Long templateId, List<ResourceInfo> resourceInfos) {
        return new InternalTemplateInfo()
                .withTemplateId(templateId)
                .withResources(resourceInfos);
    }

    private static ResourceInfo createResourceInfo(Long id, ResourceType type) {
        return new ResourceInfo()
                .withId(id)
                .withType(type);
    }
}
