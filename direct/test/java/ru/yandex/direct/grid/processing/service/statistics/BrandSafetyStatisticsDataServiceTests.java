package ru.yandex.direct.grid.processing.service.statistics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.core.entity.statistics.brandsafety.model.GdiBrandSafetyStatsRow;
import ru.yandex.direct.grid.core.entity.statistics.repository.BrandSafetyStatsYtRepository;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.statistics.brandsafety.GdBrandSafetyStatsOrderBy;
import ru.yandex.direct.grid.processing.model.statistics.brandsafety.GdBrandSafetyStatsOrderByField;
import ru.yandex.direct.grid.processing.model.statistics.brandsafety.GdBrandSafetyStatsRequest;
import ru.yandex.direct.grid.processing.model.statistics.brandsafety.GdBrandSafetyStatsTotalRequest;
import ru.yandex.direct.grid.processing.service.statistics.validation.BrandSafetyStatisticsValidationService;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;
import ru.yandex.direct.web.core.model.retargeting.CryptaGoalWeb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
public class BrandSafetyStatisticsDataServiceTests {

    private static GdClientInfo clientInfo;
    private static ClientId clientId;
    private static Integer shardId;
    private static List<Long> cids;
    private static List<Long> orderIds;
    private static List<Campaign> campaigns;
    private static List<Long> goalIds;

    @Mock
    private BrandSafetyStatisticsValidationService brandSafetyStatisticsValidationService;

    @Mock
    private BrandSafetyStatsYtRepository brandSafetyStatsYtRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CryptaService cryptaService;

    @Mock
    private ShardHelper shardHelper;

    @Spy
    @InjectMocks
    private BrandSafetyStatisticsDataService showConditionDataService;


    @BeforeClass
    public static void initTestData() {
        clientId = ClientId.fromLong(12345L);
        clientInfo = new GdClientInfo().withId(clientId.asLong());
        shardId = 1;
        cids = List.of(123L, 234L);
        orderIds = List.of(456L, 567L);
        campaigns = List.of(
                new Campaign().withId(cids.get(0)).withOrderId(orderIds.get(0)),
                new Campaign().withId(cids.get(1)).withOrderId(orderIds.get(1)));
        goalIds = List.of(1000006L, 1000007L, 1000008L);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(shardId)
                .when(shardHelper)
                .getShardByClientIdStrictly(eq(clientId));

        doReturn(List.of(
                new CryptaGoalWeb().withKeywordValue("1").withId(goalIds.get(0)),
                new CryptaGoalWeb().withKeywordValue("2").withId(goalIds.get(1)),
                new CryptaGoalWeb().withKeywordValue("4").withId(goalIds.get(2))))
                .when(cryptaService)
                .getBrandSafety();
    }

    @Test
    public void getBrandSafetyStats_cids_returnData() {
        var gdBrandSafetyStatsRequest = getGdBrandSafetyStatsRequest();

        doReturn(campaigns)
                .when(campaignRepository)
                .getCampaignsForClient(eq(shardId), eq(clientId), eq(cids));

        doReturn(List.of(
                new GdiBrandSafetyStatsRow().withOrderId(orderIds.get(0)).withDate(1610473043L).withCategory(1L),
                new GdiBrandSafetyStatsRow().withOrderId(orderIds.get(1)).withDate(1610573043L).withCategory(2L)))
                .when(brandSafetyStatsYtRepository)
                .getStats(any());

        var result = showConditionDataService.getBrandSafetyStats(clientInfo, gdBrandSafetyStatsRequest);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getGoalId()).isEqualTo(goalIds.get(0));
        assertThat(result.get(1).getGoalId()).isEqualTo(goalIds.get(1));
    }

    @Test
    public void getBrandSafetyStats_noCids_returnData() {
        var gdBrandSafetyStatsRequest = getGdBrandSafetyStatsRequest().withCids(null);

        doReturn(Map.of(orderIds.get(0), new Campaign().withId(cids.get(0)),
                orderIds.get(1), new Campaign().withId(cids.get(1))))
                .when(campaignRepository)
                .getCampaignsForOrderIds(eq(shardId), eq(orderIds));

        doReturn(List.of(
                new GdiBrandSafetyStatsRow().withOrderId(orderIds.get(0)).withDate(1610473043L).withCategory(1L),
                new GdiBrandSafetyStatsRow().withOrderId(orderIds.get(1)).withDate(1610573043L).withCategory(2L)))
                .when(brandSafetyStatsYtRepository)
                .getStats(any());

        var result = showConditionDataService.getBrandSafetyStats(clientInfo, gdBrandSafetyStatsRequest);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getGoalId()).isEqualTo(goalIds.get(0));
        assertThat(result.get(1).getGoalId()).isEqualTo(goalIds.get(1));
    }

    @Test
    public void getBrandSafetyStatsTotal_cids_returnData() {
        var gdBrandSafetyStatsTotalRequest = getGdBrandSafetyStatsTotalRequest();

        doReturn(campaigns)
                .when(campaignRepository)
                .getCampaignsForClient(eq(shardId), eq(clientId), eq(cids));

        double total = 50;
        doReturn(total)
                .when(brandSafetyStatsYtRepository)
                .getStatsTotal(any());

        var result = showConditionDataService.getBrandSafetyStatsTotal(clientInfo, gdBrandSafetyStatsTotalRequest);

        assertThat(result).isEqualTo(total);
    }

    @Test
    public void getBrandSafetyStatsTotal_noCids_returnData() {
        var gdBrandSafetyStatsTotalRequest = getGdBrandSafetyStatsTotalRequest().withCids(null);

        double total = 50;
        doReturn(total)
                .when(brandSafetyStatsYtRepository)
                .getStatsTotal(any());

        var result = showConditionDataService.getBrandSafetyStatsTotal(clientInfo, gdBrandSafetyStatsTotalRequest);

        assertThat(result).isEqualTo(total);
    }

    private static GdBrandSafetyStatsRequest getGdBrandSafetyStatsRequest() {
        return new GdBrandSafetyStatsRequest()
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withShowCampaigns(true)
                .withShowCategories(true)
                .withCids(cids)
                .withLimitOffset(new GdLimitOffset().withLimit(100).withOffset(0))
                .withOrderBy(new GdBrandSafetyStatsOrderBy()
                        .withField(GdBrandSafetyStatsOrderByField.DATE)
                        .withOrder(Order.ASC));
    }

    private static GdBrandSafetyStatsTotalRequest getGdBrandSafetyStatsTotalRequest() {
        return new GdBrandSafetyStatsTotalRequest()
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withCids(cids);
    }
}
