package ru.yandex.direct.jobs.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.model.old.OldInternalBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.internalads.model.BannerUnreachableUrl;
import ru.yandex.direct.core.entity.internalads.repository.BannersUnreachableUrlYtRepository;
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.InternalBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.internal.model.StructureOfUnavailableBanners;
import ru.yandex.direct.jobs.internal.utils.InfoForUrlNotificationsGetter;

import static freemarker.template.utility.Collections12.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_ERRORS_NOT_DISABLE;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_IDS_NOT_DISABLE;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_STOP_BANNERS_ENABLED;
import static ru.yandex.direct.common.db.PpcPropertyNames.BANNERS_UNREACHABLE_URL_URLS_NOT_DISABLE;
import static ru.yandex.direct.core.entity.internalads.repository.BannersUnreachableUrlYtRepository.createBannerUnreachableUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeInternalBanner;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_4_URL_IMG;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_4_RESOURCE_2_REQUIRED_URL;

@JobsTest
@ExtendWith(SpringExtension.class)
public class UpdateBannersUnreachableUrlJobStopBannersTest {
    private static final int BANNERS_COUNT = 3;

    private static final List<String> BANNERS_URL =
            List.of("https://ya.ru", "https://2345.ru", "https://abcde23456.ru");

    private static final List<String> BANNERS_REASON = List.of("404", "505", "666");

    private static final List<Boolean> BANNERS_STATUS_SHOW = List.of(true, true, false);

    private List<BannerUnreachableUrl> bannersUnreachableUrls;

    private Map<Integer, List<Long>> bannerIdsByShard;

    private Map<Long, Boolean> statusShowById;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private BannerModifyRepository modifyRepository;

    @Mock
    private BannersUnreachableUrlYtRepository ytRepository;

    @Mock
    private BannerUrlCheckService bannerUrlCheckService;

    @Mock
    private UrlMonitoringNotifyService urlMonitoringNotifyService;

    @Mock
    private InfoForUrlNotificationsGetter infoForUrlNotificationsGetter;

    @Captor
    private ArgumentCaptor<List<StructureOfUnavailableBanners>> urlByBannerIdStoppedCaptor;

    private UpdateBannersUnreachableUrlJob job;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);

        initTestFields();

        doReturn(bannersUnreachableUrls)
                .when(ytRepository).getAll();

        doReturn(new UrlCheckResult(false, UrlCheckResult.Error.HTTP_ERROR))
                .when(bannerUrlCheckService).isUrlReachable(anyString());

        initSetPropertiesDefault();

        job = Mockito.spy(new UpdateBannersUnreachableUrlJob(shardHelper, ppcDslContextProvider, ppcPropertiesSupport,
                ytRepository, modifyRepository, bannerUrlCheckService, urlMonitoringNotifyService,
                infoForUrlNotificationsGetter));
    }

    @Test
    void checkStopBanners_StopBannersEnabled() {
        checkStopBanners(banner -> {
            assertThat(banner.getStatusShow()).isFalse();
            if (statusShowById.get(banner.getId())) {
                assertThat(banner.getStatusBsSynced())
                        .isEqualTo(StatusBsSynced.NO);
                assertThat(banner.getIsStoppedByUrlMonitoring())
                        .isTrue();
            }
        });
    }

    @Test
    void checkStopBanners_StopBannersDisabled() {
        ppcPropertiesSupport.get(BANNERS_UNREACHABLE_URL_STOP_BANNERS_ENABLED).set(false);
        checkStopBanners(banner -> {
                    assertThat(banner.getStatusShow())
                            .isEqualTo(statusShowById.get(banner.getId()));
                }
        );
    }

    @Test
    void checkStopBanners_NotStoppedExpectedBanners() {
        BannerUnreachableUrl banner = bannersUnreachableUrls.get(0);
        bannersUnreachableUrls.set(0,
                createBannerUnreachableUrl(banner.getId(), "https://otherurl.ru", banner.getReason())
        );
        doAnswer(invocation -> {
            List<BannerUnreachableUrl> banners = invocation.getArgument(0);
            return List.of(new StructureOfUnavailableBanners(11L, "11", Map.of(1L, banners)));
        }).when(job).expandInfoOfUnavailableBanners(anyList());

        job.execute();

        verify(urlMonitoringNotifyService).notifyBannersStopped(urlByBannerIdStoppedCaptor.capture(), eq(true));

        List<StructureOfUnavailableBanners> structuresOfStoppedBanners = urlByBannerIdStoppedCaptor.getValue();

        List<BannerUnreachableUrl> flattenBanners = StreamEx.of(structuresOfStoppedBanners)
                .flatMap(structureOfUnavailableBanners ->
                        EntryStream.of(structureOfUnavailableBanners.getBannersByTemplateId())
                                .values()
                                .flatMap(List::stream)
                )
                .toList();


        assertThat(flattenBanners)
                .isEqualTo(List.of(
                        createBannerUnreachableUrl(
                                bannersUnreachableUrls.get(1).getId(),
                                bannersUnreachableUrls.get(1).getUrl(),
                                bannersUnreachableUrls.get(1).getReason()
                        )
                ));
    }

    private void checkStopBanners(Consumer<InternalBanner> checkingAction) {
        job.execute();

        for (var shardAndId : bannerIdsByShard.entrySet()) {
            var banners = bannerTypedRepository.getStrictlyFullyFilled(shardAndId.getKey(),
                    shardAndId.getValue(), InternalBanner.class);
            banners.forEach(checkingAction);
        }
    }

    private void initTestFields() {
        List<Map.Entry<Integer, Long>> shardAndBannerIdList = getShardAndBannerIdList();

        bannersUnreachableUrls = IntStreamEx.range(0, BANNERS_COUNT)
                .mapToObj(i -> createBannerUnreachableUrl(
                        shardAndBannerIdList.get(i).getValue(),
                        BANNERS_URL.get(i),
                        BANNERS_REASON.get(i))
                )
                .toList();

        bannerIdsByShard = EntryStream.of(shardAndBannerIdList.stream())
                .grouping();

        statusShowById = IntStreamEx.range(0, BANNERS_COUNT)
                .mapToEntry(i -> shardAndBannerIdList.get(i).getValue(), BANNERS_STATUS_SHOW::get)
                .toMap();
    }

    private void initSetPropertiesDefault() {
        ppcPropertiesSupport.get(BANNERS_UNREACHABLE_URL_STOP_BANNERS_ENABLED).set(true);
        initSetPropertyDefault(BANNERS_UNREACHABLE_URL_IDS_NOT_DISABLE);
        initSetPropertyDefault(BANNERS_UNREACHABLE_URL_URLS_NOT_DISABLE);
        initSetPropertyDefault(BANNERS_UNREACHABLE_URL_ERRORS_NOT_DISABLE);
    }

    private <T> void initSetPropertyDefault(PpcPropertyName<Set<T>> ppcPropertyName) {
        ppcPropertiesSupport.get(ppcPropertyName).set(Collections.emptySet());
    }

    private List<Map.Entry<Integer, Long>> getShardAndBannerIdList() {
        return IntStreamEx.range(0, BANNERS_COUNT)
                .mapToEntry(BANNERS_URL::get, BANNERS_STATUS_SHOW::get)
                .map(this::createInternalBanner)
                .mapToEntry(InternalBannerInfo::getShard, InternalBannerInfo::getBannerId)
                .toList();
    }

    private InternalBannerInfo createInternalBanner(Map.Entry<String, Boolean> urlAndStatusShow) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup();
        OldInternalBanner bannerWithUrl = activeInternalBanner(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId())
                .withTemplateId(PLACE_1_TEMPLATE_4_URL_IMG)
                .withTemplateVariables(singletonList(
                        new TemplateVariable()
                                .withTemplateResourceId(TEMPLATE_4_RESOURCE_2_REQUIRED_URL)
                                .withInternalValue(urlAndStatusShow.getKey())))
                .withStatusShow(urlAndStatusShow.getValue());

        return steps.bannerSteps().createActiveInternalBanner(adGroupInfo, bannerWithUrl);
    }

}
