package ru.yandex.direct.grid.core.entity.banner.service;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.banner.type.banneradditions.BannerAdditionsRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.AdGroupIdInFilterBaseTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerFilter;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderBy;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannersWithTotals;
import ru.yandex.direct.grid.core.entity.banner.repository.GridBannerAdditionsRepository;
import ru.yandex.direct.grid.core.entity.banner.repository.GridBannerDomainRepository;
import ru.yandex.direct.grid.core.entity.banner.repository.GridBannerRepository;
import ru.yandex.direct.grid.core.entity.banner.repository.GridBannerYtRepository;
import ru.yandex.direct.grid.core.entity.banner.repository.GridImageRepository;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdFetchedFieldsResolver;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GridBannerServiceAdGroupIdInFilterTest extends AdGroupIdInFilterBaseTest {

    private static final int SHARD = 1;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1);
    private static final Long OPERATOR_UID = 3L;
    private static final List<GdiBannerOrderBy> BANNER_ORDER_BY_LIST = emptyList();
    private static final LocalDate STAT_FROM = LocalDate.now();
    private static final LocalDate STAT_TO = LocalDate.now();
    private static final AdFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER =
            FetchedFieldsResolverCoreUtil.buildAdFetchedFieldsResolver(true);
    private GdiBannerFilter filter;

    @Mock
    private GridBannerYtRepository gridBannerYtRepository;

    @Mock
    private GridBannerRepository gridBannerRepository;

    @Mock
    private GridImageRepository gridImageRepository;

    @Mock
    private GridBannerDomainRepository gridBannerDomainRepository;

    @Mock
    private BannerAdditionsRepository bannerAdditionsRepository;

    @Mock
    private CalloutRepository calloutRepository;

    @Mock
    private GridBannerAdditionsRepository gridBannerAdditionsRepository;

    @InjectMocks
    private GridBannerService gridBannerService;

    @Captor
    private ArgumentCaptor<GdiBannerFilter> filterArgumentCaptor;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

        filter = new GdiBannerFilter()
                .withAdGroupIdIn(adGroupIdIn)
                .withCampaignIdIn(campaignIdIn);

        doReturn(new GdiBannersWithTotals().withGdiBanners(emptyList())).when(gridBannerYtRepository)
                .getBanners(eq(SHARD), eq(filter), eq(BANNER_ORDER_BY_LIST), eq(STAT_FROM), eq(STAT_TO), any(), any(),
                        eq(emptySet()), isNull(), eq(STAT_FROM), eq(STAT_TO), any(), eq(emptySet()), anyBoolean());
    }


    @Test
    public void getBannersTest() {
        if (expectedException) {
            checkGetBannersThrowException();
            return;
        }

        gridBannerService.getBanners(SHARD, OPERATOR_UID, CLIENT_ID, emptySet(),
                filter, BANNER_ORDER_BY_LIST, STAT_FROM, STAT_TO,
                null, emptySet(), STAT_FROM, STAT_TO, AD_FETCHED_FIELDS_RESOLVER, false);

        verify(gridBannerYtRepository)
                .getBanners(eq(SHARD), filterArgumentCaptor.capture(), eq(BANNER_ORDER_BY_LIST),
                        eq(STAT_FROM), eq(STAT_TO), any(), any(), eq(emptySet()), isNull(),
                        eq(STAT_FROM), eq(STAT_TO), any(), eq(emptySet()), anyBoolean());

        assertThat(filterArgumentCaptor.getValue().getAdGroupIdIn())
                .isEqualTo(expectedAdGroupIdIn);
    }

    private void checkGetBannersThrowException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("campaignIdIn from filter must be not empty, when count of adGroupIdIn over limit");

        gridBannerService.getBanners(SHARD, OPERATOR_UID, CLIENT_ID, emptySet(),
                filter, BANNER_ORDER_BY_LIST, STAT_FROM, STAT_TO,
                null, emptySet(), STAT_FROM, STAT_TO, AD_FETCHED_FIELDS_RESOLVER, false);
    }

}
