package ru.yandex.direct.jobs.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.internalads.model.BannerUnreachableUrl;
import ru.yandex.direct.core.entity.internalads.repository.BannersUnreachableUrlYtRepository;
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.internal.model.StructureOfUnavailableBanners;
import ru.yandex.direct.jobs.internal.utils.InfoForUrlNotificationsGetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_ERRORS_NOT_DISABLE;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_IDS_NOT_DISABLE;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_STOP_BANNERS_ENABLED;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_URLS_NOT_DISABLE;
import static ru.yandex.direct.core.entity.internalads.repository.BannersUnreachableUrlYtRepository.createBannerUnreachableUrl;

@ParametersAreNonnullByDefault
public class UpdateBannersUnreachableUrlJobTest {
    private static final Comparator<BannerUnreachableUrl> BANNER_COMPARATOR =
            Comparator.comparingLong(BannerUnreachableUrl::getId)
                    .thenComparing(BannerUnreachableUrl::getUrl)
                    .thenComparing(BannerUnreachableUrl::getReason);

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private DslContextProvider ppcDslContextProvider;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private BannersUnreachableUrlYtRepository ytRepository;

    @Mock
    private BannerModifyRepository modifyRepository;

    @Mock
    private BannerUrlCheckService bannerUrlCheckService;

    @Mock
    private UrlMonitoringNotifyService urlMonitoringNotifyService;

    @Mock
    private PpcProperty<Boolean> ppcPropertyIsStopBannersEnabled;

    @Mock
    private PpcProperty<Set<Long>> ppcPropertyIdsNotDisable;

    @Mock
    private PpcProperty<Set<String>> ppcPropertyUrlsNotDisable;

    @Mock
    private PpcProperty<Set<String>> ppcPropertyErrorsNotDisable;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private BannerTypedRepository bannerTypedRepository;

    @InjectMocks
    private InfoForUrlNotificationsGetter infoForUrlNotificationsGetter;

    private UpdateBannersUnreachableUrlJob job;

    @Captor
    private ArgumentCaptor<List<BannerUnreachableUrl>> bannersCaptor;

    @Captor
    private ArgumentCaptor<List<StructureOfUnavailableBanners>> bannerStructuresCaptor;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);

        job = Mockito.spy(new UpdateBannersUnreachableUrlJob(
                shardHelper, ppcDslContextProvider, ppcPropertiesSupport, ytRepository, modifyRepository,
                bannerUrlCheckService, urlMonitoringNotifyService, infoForUrlNotificationsGetter
        ));

        doAnswer(invocation -> Map.of(1, invocation.getArgument(0))).when(shardHelper).getBannerIdsByShard(anyList());

        doReturn(getBannersUnreachableUrlTestData())
                .when(ytRepository).getAll();

        initBannerUrlCheckService(getBannersUnreachableUrlTestData(), getUrlCheckResultMapTestData());

        doReturn(true)
                .when(ppcPropertyIsStopBannersEnabled).getOrDefault(anyBoolean());

        doReturn(ppcPropertyIsStopBannersEnabled)
                .when(ppcPropertiesSupport).get(BANNERS_UNREACHABLE_URL_STOP_BANNERS_ENABLED);

        doReturn(getStoppedBannersTestData())
                .when(job).getStoppedBanners(anyList(), anyBoolean());

        doAnswer(getInternalBannersAnswer).when(bannerTypedRepository).getInternalBannersByIds(anyInt(), anySet());

        doAnswer(invocation -> {
            List<Long> campaignIds = invocation.getArgument(1);
            return EntryStream.of(getCampaignsTestData())
                    .filterKeys(campaignIds::contains)
                    .toMap();
        }).when(campaignRepository).getCampaignsSimple(anyInt(), anyList());

        initSetPropertiesDefault();
    }

    @Test
    void checkJob() {
        job.execute();

        verify(ytRepository).getAll();

        int urlsCount = getDistinctUrls(ytRepository.getAll()).size();
        verify(bannerUrlCheckService, times(urlsCount)).isUrlReachable(anyString());

        verify(urlMonitoringNotifyService).notifyBannersStopped(anyList(), anyBoolean());
        verify(urlMonitoringNotifyService).notifyBannersNotDisable(anyList(), any(), any(), any());
    }

    @Test
    void checkGetUnreachableBanners() {
        initSetPropertiesTestData();

        job.execute();

        verify(job).getFilteredUnreachableBanners(bannersCaptor.capture(), any(), any(), any());

        List<BannerUnreachableUrl> unreachableUrls =
                job.getUnreachableBanners(bannersCaptor.getValue(), getUrlCheckResultMapTestData());

        List<BannerUnreachableUrl> expected = Arrays.asList(
                createBannerUnreachableUrl(1322837L, "https://direct.ru", "500"),
                createBannerUnreachableUrl(333L, "https://direct.ru", "500"),

                createBannerUnreachableUrl(5L, "https://link.ru", "404"),
                createBannerUnreachableUrl(4L, "https://link.ru", "404"),
                createBannerUnreachableUrl(333L, "https://link.ru", "404"),

                createBannerUnreachableUrl(5L, "https://123456789.ru", "496"),
                createBannerUnreachableUrl(1322837L, "https://123456789.ru", "496"),

                createBannerUnreachableUrl(10L, "https://abcdefgh.ru", "505"),

                createBannerUnreachableUrl(1322837L, "https://magic.ru", "666"),
                createBannerUnreachableUrl(890L, "https://magic.ru", "666"),

                createBannerUnreachableUrl(999L, "https://sweets.ru", "999"),
                createBannerUnreachableUrl(890L, "https://sweets.ru", "999"),
                createBannerUnreachableUrl(5L, "https://sweets.ru", "999"),
                createBannerUnreachableUrl(2L, "https://sweets.ru", "999"),

                createBannerUnreachableUrl(444L, "https://translate.yandex.ru/", "123"),
                createBannerUnreachableUrl(444L, "https://yandex.ru/pogoda/saint-petersburg", "123")
        );

        assertBannersEqual(unreachableUrls, expected);
    }

    @Test
    void checkGetFilteredUnreachableBanners() {
        initSetPropertiesTestData();

        job.execute();

        verify(job).getStoppedBanners(bannersCaptor.capture(), anyBoolean());

        assertBannersEqual(bannersCaptor.getValue(), getStoppedBannersTestData());
    }

    @Test
    void checkNotDisableBanners() {
        initSetPropertiesTestData();

        job.execute();

        verify(urlMonitoringNotifyService).notifyBannersNotDisable(bannerStructuresCaptor.capture(), any(), any(),
                any());

        List<StructureOfUnavailableBanners> expected = List.of(
                new StructureOfUnavailableBanners(
                        11L, "camp11",
                        Map.of(1L, List.of(
                                createBannerUnreachableUrl(1322837L, "https://direct.ru", "500"),
                                createBannerUnreachableUrl(1322837L, "https://123456789.ru", "496"),
                                createBannerUnreachableUrl(1322837L, "https://magic.ru", "666")
                        ))
                ),
                new StructureOfUnavailableBanners(
                        22L, "camp22",
                        Map.of(4L, List.of(
                                createBannerUnreachableUrl(10L, "https://abcdefgh.ru", "505"),
                                createBannerUnreachableUrl(999L, "https://sweets.ru", "999")
                        ))
                ),
                new StructureOfUnavailableBanners(
                        33L, "camp33",
                        Map.of(5L, List.of(
                                createBannerUnreachableUrl(2L, "https://sweets.ru", "999")
                        ))
                )
        );

        assertThat(bannerStructuresCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @ParameterizedTest
    @MethodSource("testDataNotDisableBanners")
    void checkNotDisableBanners(List<BannerUnreachableUrl> unreachableBanners,
                                List<BannerUnreachableUrl> filteredUnreachableBanners,
                                List<BannerUnreachableUrl> stoppedBanners,
                                List<BannerUnreachableUrl> expectedNotDisableBanners) {
        assertBannersEqual(job.getNotDisableBanners(unreachableBanners, filteredUnreachableBanners, stoppedBanners),
                expectedNotDisableBanners);
    }

    static Object[] testDataNotDisableBanners() {
        return new Object[][]{
                {
                        List.of(
                                createBannerUnreachableUrl(1L, "u1", "r1"),
                                createBannerUnreachableUrl(1L, "u2", "r2")
                        ),
                        List.of(createBannerUnreachableUrl(1L, "u1", "r1")),
                        Collections.emptyList(),
                        Arrays.asList(createBannerUnreachableUrl(1L, "u2", "r2"))
                },
                {
                        List.of(
                                createBannerUnreachableUrl(1L, "u1", "r1"),
                                createBannerUnreachableUrl(1L, "u2", "r2")
                        ),
                        List.of(createBannerUnreachableUrl(1L, "u1", "r1")),
                        List.of(createBannerUnreachableUrl(1L, "u1", "r1")),
                        Collections.emptyList()
                },
                {
                        List.of(createBannerUnreachableUrl(1L, "u1", "r1")),
                        List.of(createBannerUnreachableUrl(1L, "u1", "r1")),
                        Collections.emptyList(),
                        Collections.emptyList()
                }
        };
    }

    @Test
    void checkGetUrlsByBannerId() {
        List<BannerUnreachableUrl> testData = List.of(
                createBannerUnreachableUrl(1322837L, "https://direct.ru", "500"),
                createBannerUnreachableUrl(333L, "https://direct.ru", "500"),

                createBannerUnreachableUrl(1322837L, "https://ya.ru", "404"),
                createBannerUnreachableUrl(4L, "https://ya.ru", "404"),
                createBannerUnreachableUrl(890L, "https://ya.ru", "404"),

                createBannerUnreachableUrl(5L, "https://link.ru", "404"),
                createBannerUnreachableUrl(4L, "https://link.ru", "404"),
                createBannerUnreachableUrl(333L, "https://link.ru", "404")
        );
        Map<Long, Set<String>> urlsByBannerId = job.getUrlsByBannerId(testData);

        var expected = Map.of(
                1322837L, Set.of("https://direct.ru", "https://ya.ru"),
                333L, Set.of("https://direct.ru", "https://link.ru"),
                4L, Set.of("https://ya.ru", "https://link.ru"),
                5L, Set.of("https://link.ru"),
                890L, Set.of("https://ya.ru")
        );

        assertThat(urlsByBannerId)
                .isEqualTo(expected);
    }

    @Test
    void checkExpandInfoOfUnavailableBanners() {
        List<BannerUnreachableUrl> testData = List.of(
                createBannerUnreachableUrl(1322837L, "https://direct.ru", "500"),
                createBannerUnreachableUrl(5L, "https://link.ru", "404"),
                createBannerUnreachableUrl(999L, "https://lalala.ru", "999"),
                createBannerUnreachableUrl(1322837L, "https://ya.ru", "404"),
                createBannerUnreachableUrl(101L, "https://batman.ru", "123"),
                createBannerUnreachableUrl(101L, "https://java.ru", "456"),
                createBannerUnreachableUrl(444L, "https://translate.yandex.ru/", "123")
        );

        List<StructureOfUnavailableBanners> actual = job.expandInfoOfUnavailableBanners(testData);

        List<StructureOfUnavailableBanners> expected = List.of(
                new StructureOfUnavailableBanners(11L, "camp11", Map.of(
                        1L, List.of(
                                createBannerUnreachableUrl(1322837L, "https://direct.ru", "500"),
                                createBannerUnreachableUrl(1322837L, "https://ya.ru", "404")
                        ),
                        2L, List.of(
                                createBannerUnreachableUrl(5L, "https://link.ru", "404")
                        )
                )),
                new StructureOfUnavailableBanners(22L, "camp22", Map.of(
                        4L, List.of(
                                createBannerUnreachableUrl(999L, "https://lalala.ru", "999")
                        )
                )),
                new StructureOfUnavailableBanners(33L, "camp33", Map.of(
                        5L, List.of(
                                createBannerUnreachableUrl(101L, "https://batman.ru", "123"),
                                createBannerUnreachableUrl(101L, "https://java.ru", "456"),
                                createBannerUnreachableUrl(444L, "https://translate.yandex.ru/", "123")
                        )
                ))
        );

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static void assertBannersEqual(List<BannerUnreachableUrl> actual, List<BannerUnreachableUrl> expected) {
        assertThat(getSortedBanners(actual))
                .isEqualTo(getSortedBanners(expected));
    }

    private static List<BannerUnreachableUrl> getSortedBanners(List<BannerUnreachableUrl> banners) {
        Collections.sort(banners, BANNER_COMPARATOR);
        return banners;
    }

    private void initBannerUrlCheckService(List<BannerUnreachableUrl> banners,
                                           Map<String, UrlCheckResult> urlCheckResultMap) {
        List<String> urls = getDistinctUrls(banners);
        for (String url : urls) {
            doReturn(urlCheckResultMap.get(url))
                    .when(bannerUrlCheckService).isUrlReachable(url);
        }
    }

    private List<String> getDistinctUrls(List<BannerUnreachableUrl> banners) {
        return StreamEx.of(banners)
                .map(BannerUnreachableUrl::getUrl)
                .distinct()
                .toList();
    }

    private void initSetPropertiesDefault() {
        initSetProperty(ppcPropertyIdsNotDisable, BANNERS_UNREACHABLE_URL_IDS_NOT_DISABLE, Collections.emptySet());
        initSetProperty(ppcPropertyUrlsNotDisable, BANNERS_UNREACHABLE_URL_URLS_NOT_DISABLE, Collections.emptySet());
        initSetProperty(ppcPropertyErrorsNotDisable, BANNERS_UNREACHABLE_URL_ERRORS_NOT_DISABLE,
                Collections.emptySet());
    }

    private void initSetPropertiesTestData() {
        initSetProperty(ppcPropertyIdsNotDisable, BANNERS_UNREACHABLE_URL_IDS_NOT_DISABLE, getIdsNotDisableTestData());
        initSetProperty(ppcPropertyUrlsNotDisable, BANNERS_UNREACHABLE_URL_URLS_NOT_DISABLE,
                getUrlsNotDisableTestData());
        initSetProperty(ppcPropertyErrorsNotDisable, BANNERS_UNREACHABLE_URL_ERRORS_NOT_DISABLE,
                getErrorsNotDisableTestData());
    }

    private <T> void initSetProperty(PpcProperty<Set<T>> ppcSetProperty, PpcPropertyName<Set<T>> ppcPropertyName,
                                     Set<T> returnValue) {
        doReturn(returnValue)
                .when(ppcSetProperty).getOrDefault(anySet());

        doReturn(ppcSetProperty)
                .when(ppcPropertiesSupport).get(ppcPropertyName);
    }

    private static List<BannerUnreachableUrl> getBannersUnreachableUrlTestData() {
        return List.of(
                createBannerUnreachableUrl(1322837L, "https://direct.ru", "500"),
                createBannerUnreachableUrl(333L, "https://direct.ru", "500"),

                createBannerUnreachableUrl(1322837L, "https://ya.ru", "404"),
                createBannerUnreachableUrl(4L, "https://ya.ru", "404"),
                createBannerUnreachableUrl(890L, "https://ya.ru", "404"),

                createBannerUnreachableUrl(5L, "https://link.ru", "404"),
                createBannerUnreachableUrl(4L, "https://link.ru", "404"),
                createBannerUnreachableUrl(333L, "https://link.ru", "404"),

                createBannerUnreachableUrl(5L, "https://123456789.ru", "496"),
                createBannerUnreachableUrl(1322837L, "https://123456789.ru", "496"),

                createBannerUnreachableUrl(10L, "https://abcdefgh.ru", "505"),

                createBannerUnreachableUrl(1322837L, "https://magic.ru", "666"),
                createBannerUnreachableUrl(890L, "https://magic.ru", "666"),

                createBannerUnreachableUrl(999L, "https://lalala.ru", "999"),
                createBannerUnreachableUrl(1L, "https://lalala.ru", "999"),
                createBannerUnreachableUrl(1322837L, "https://lalala.ru", "999"),

                createBannerUnreachableUrl(999L, "https://sweets.ru", "999"),
                createBannerUnreachableUrl(890L, "https://sweets.ru", "999"),
                createBannerUnreachableUrl(5L, "https://sweets.ru", "999"),
                createBannerUnreachableUrl(2L, "https://sweets.ru", "999"),

                createBannerUnreachableUrl(101L, "https://batman.ru", "123"),
                createBannerUnreachableUrl(333L, "https://batman.ru", "123"),

                createBannerUnreachableUrl(101L, "https://java.ru", "456"),
                createBannerUnreachableUrl(10L, "https://java.ru", "456"),
                createBannerUnreachableUrl(1L, "https://java.ru", "456"),

                createBannerUnreachableUrl(444L, "https://translate.yandex.ru/", "123"),
                createBannerUnreachableUrl(444L, "https://yandex.ru/pogoda/saint-petersburg", "123")
        );
    }

    private static Map<Long, CampaignSimple> getCampaignsTestData() {
        return StreamEx.of(List.of(
                createCampaign(11L, "camp11"),
                createCampaign(22L, "camp22"),
                createCampaign(33L, "camp33")
        )).toMap(CampaignSimple::getId, Function.identity());
    }

    private static Map<Long, Map<Long, List<Long>>> getStructureOfBanners() {
        return Map.of(
                11L, Map.of(
                        1L, List.of(1322837L, 333L, 4L, 890L),
                        2L, List.of(5L)
                ),
                22L, Map.of(
                        4L, List.of(10L, 999L)
                ),
                33L, Map.of(
                        5L, List.of(1L, 2L, 101L, 444L)
                )
        );
    }

    private static List<BannerUnreachableUrl> getStoppedBannersTestData() {
        return Arrays.asList(
                createBannerUnreachableUrl(4L, "https://link.ru", "404"),
                createBannerUnreachableUrl(5L, "https://link.ru", "404"),
                createBannerUnreachableUrl(333L, "https://link.ru", "404"),

                createBannerUnreachableUrl(5L, "https://123456789.ru", "496"),

                createBannerUnreachableUrl(5L, "https://sweets.ru", "999"),
                createBannerUnreachableUrl(890L, "https://sweets.ru", "999"),

                createBannerUnreachableUrl(444L, "https://yandex.ru/pogoda/saint-petersburg", "123")
        );
    }

    private static Map<String, UrlCheckResult> getUrlCheckResultMapTestData() {
        Map<String, UrlCheckResult> map = new HashMap<>();
        map.put("https://direct.ru", new UrlCheckResult(false, UrlCheckResult.Error.HTTP_ERROR));
        map.put("https://ya.ru", new UrlCheckResult(true, null));
        map.put("https://link.ru", new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT));
        map.put("https://123456789.ru", new UrlCheckResult(false, UrlCheckResult.Error.TOO_MANY_REDIRECTS));
        map.put("https://abcdefgh.ru", new UrlCheckResult(false, UrlCheckResult.Error.HTTP_ERROR));
        map.put("https://magic.ru", new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT));
        map.put("https://lalala.ru", new UrlCheckResult(true, null));
        map.put("https://sweets.ru", new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT));
        map.put("https://batman.ru", new UrlCheckResult(true, null));
        map.put("https://java.ru", new UrlCheckResult(true, null));
        map.put("https://translate.yandex.ru/", new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT));
        map.put("https://yandex.ru/pogoda/saint-petersburg", new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT));
        return map;
    }

    private static Set<Long> getIdsNotDisableTestData() {
        return Set.of(1322837L, 1L, 2L, 999L, 228L);
    }

    private static Set<String> getUrlsNotDisableTestData() {
        return Set.of("https://ya.ru", "https://abcdefgh.ru", "magic", "translate");
    }

    private static Set<String> getErrorsNotDisableTestData() {
        return Set.of("HTTP_ERROR", "INVALID_NAME");
    }

    private static CampaignSimple createCampaign(Long id, String name) {
        return new Campaign()
                .withId(id)
                .withName(name);
    }

    private static InternalBanner createInternalBanner(long id, long campaignId, long tempalteId) {
        return new InternalBanner()
                .withId(id)
                .withCampaignId(campaignId)
                .withTemplateId(tempalteId);
    }

    private final Answer<List<InternalBanner>> getInternalBannersAnswer = (InvocationOnMock invocation) -> {
        Set<Long> targetIds = new HashSet<>(invocation.getArgument(1));
        Map<Long, Map<Long, List<Long>>> structure = getStructureOfBanners();
        LinkedList<InternalBanner> internalBanners = new LinkedList<>();

        EntryStream.of(structure)
                .forKeyValue((campaignId, bannerIdsByTemplateId) -> EntryStream.of(bannerIdsByTemplateId)
                        .forKeyValue((templateId, bannerIds) -> StreamEx.of(bannerIds)
                                .forEach(id -> {
                                    if (!targetIds.contains(id)) {
                                        return;
                                    }
                                    internalBanners.add(createInternalBanner(id, campaignId, templateId));
                                    targetIds.remove(id);
                                })));
        return internalBanners;
    };
}
