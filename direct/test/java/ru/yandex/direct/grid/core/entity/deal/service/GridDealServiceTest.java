package ru.yandex.direct.grid.core.entity.deal.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.deal.container.DealStat;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealPlacement;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.entity.deal.service.DealService;
import ru.yandex.direct.core.entity.placements.model.Placement;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.deal.model.GdiDeal;
import ru.yandex.direct.grid.core.entity.deal.model.GdiDealPlacement;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

public class GridDealServiceTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(123L);
    private static final ClientId CLIENT_ID_WITHOUT_DEALS = ClientId.fromLong(555L);
    private static final int SHARD = 10;
    private static final String DOMAIN = "testdomain.ru";
    private static final long CLICKS = 1L;
    private static final long SHOWS = 100L;
    private static final BigDecimal SPENT = BigDecimal.valueOf(1000L);
    private static final BigDecimal CPC = BigDecimal.valueOf(1000L);
    private static final BigDecimal CPM = BigDecimal.TEN.multiply(BigDecimal.valueOf(1000));
    private static final BigDecimal CTR = BigDecimal.ONE;
    private static final Long PAGE_ID1 = 123L;
    private static final Long PAGE_ID2 = 456L;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private DealService dealService;

    @InjectMocks
    private GridDealService gridDealService;

    private Long dealId1;
    private Long dealId2;
    private String name1;
    private String name2;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        Deal deal1 = TestDeals.defaultPrivateDeal(CLIENT_ID);
        dealId1 = deal1.getId();
        name1 = deal1.getName();
        Deal deal2 = TestDeals.defaultPrivateDeal(CLIENT_ID);
        dealId2 = deal2.getId();
        name2 = deal2.getName();

        deal1.setPlacements(singletonList(new DealPlacement()
                .withPageId(PAGE_ID1)
                .withImpId(Arrays.asList(1L, 2L))));
        Placement placement1 = new Placement()
                .withPageId(PAGE_ID1)
                .withDomain(DOMAIN)
                .withBlocks(
                        "{\"1\": [\"400x200\", \"0x0\"], \"2\": [\"400x200\", \"1000x1000\"], \"3\": [\"400x200\", \"800x600\"]}");
        deal2.setPlacements(singletonList(new DealPlacement()
                .withPageId(PAGE_ID2)
                .withImpId(Arrays.asList(3L, 4L))));
        Placement placement2 = new Placement()
                .withPageId(PAGE_ID2)
                .withDomain(DOMAIN)
                .withBlocks(
                        "{\"3\": [\"450x250\", \"0x0\"], \"4\": [\"400x200\", \"1020x1020\"], \"5\": [\"400x200\", \"900x600\"]}");
        doReturn(asList(deal1, deal2)).when(dealRepository).getDealsBriefByClientId(eq(SHARD), eq(CLIENT_ID));

        Map<Long, Placement> placementsMap = new HashMap<>();
        placementsMap.put(PAGE_ID1, placement1);
        placementsMap.put(PAGE_ID2, placement2);
        doReturn(placementsMap).when(dealService).getPlacementsMapByDeals(eq(asList(deal1, deal2)));

        DealStat dealStat = new DealStat()
                .withId(dealId1)
                .withClicks(CLICKS)
                .withShows(SHOWS)
                .withSpent(SPENT)
                .withCpc(CPC)
                .withCpm(CPM)
                .withCtr(Percent.fromRatio(CTR));
        doReturn(singletonList(dealStat)).when(dealService).getDealStatistics(eq(asList(dealId1, dealId2)));

        Map<Long, List<Long>> linkedCampaigns = new HashMap<>();
        linkedCampaigns.put(dealId1, asList(1L, 2L));
        linkedCampaigns.put(dealId2, Collections.emptyList());
        doReturn(linkedCampaigns).when(dealService).getLinkedCampaignsByDealIds(eq(asList(dealId1, dealId2)));
    }

    @Test
    public void testGetDeal() {
        List<GdiDeal> deals = gridDealService.getDeals(SHARD, CLIENT_ID, true);
        assertThat(deals).size().isEqualTo(2);
        assertThat(deals)
                .is(matchedBy(beanDiffer(
                        Arrays.asList(
                                new GdiDeal()
                                        .withId(dealId1)
                                        .withName(name1)
                                        .withPlacements(singletonList(new GdiDealPlacement()
                                                .withDomain(DOMAIN)
                                                .withPageId(PAGE_ID1)
                                                .withImpId(asList(1L, 2L))
                                                .withFormats(asSet("400x200", "0x0", "1000x1000"))))
                                        .withNumberOfLinkedCampaigns(2),
                                new GdiDeal()
                                        .withId(dealId2)
                                        .withName(name2)
                                        .withPlacements(singletonList(new GdiDealPlacement()
                                                .withDomain(DOMAIN)
                                                .withPageId(PAGE_ID2)
                                                .withImpId(asList(3L, 4L))
                                                .withFormats(asSet("450x250", "0x0", "1020x1020", "400x200"))))
                                        .withNumberOfLinkedCampaigns(0)
                        )
                        ).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                ));
    }

    @Test
    public void testNeedStatsIsTrue() {
        List<GdiDeal> deals = gridDealService.getDeals(SHARD, CLIENT_ID, true);
        assertThat(deals).size().isEqualTo(2);
        GdiDeal deal = filterList(deals, d -> d.getId().equals(dealId1)).get(0);
        assertThat(deal.getCpm()).isEqualTo(CPM);
    }

    @Test
    public void testNeedStatsIsFalse() {
        List<GdiDeal> deals = gridDealService.getDeals(SHARD, CLIENT_ID, false);
        assertThat(deals).size().isEqualTo(2);
        GdiDeal deal = filterList(deals, d -> d.getId().equals(dealId1)).get(0);
        assertThat(deal.getCpm()).isNull();
    }

    @Test
    public void clientWithoutDeals() {
        List<GdiDeal> deals = gridDealService.getDeals(SHARD, CLIENT_ID_WITHOUT_DEALS, true);
        assertThat(deals).size().isEqualTo(0);

    }
}
