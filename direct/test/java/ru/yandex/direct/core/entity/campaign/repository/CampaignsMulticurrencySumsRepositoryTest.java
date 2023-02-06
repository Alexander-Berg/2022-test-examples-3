package ru.yandex.direct.core.entity.campaign.repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_MULTICURRENCY_SUMS;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignsMulticurrencySumsRepositoryTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final int TEST_SHARD = 1;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampaignsMulticurrencySumsRepository repo;

    private Long campId;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(TEST_SHARD));
        final CampaignInfo campaign = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, clientInfo);
        campId = campaign.getCampaignId();
    }

    @Test
    public void getCampaignsMulticurrencyChipsCostsTest() {
        final BigDecimal sum = BigDecimal.valueOf(123);
        final BigDecimal chipsSpent = BigDecimal.valueOf(124);
        final BigDecimal chipsCost = chipsSpent.multiply(BigDecimal.valueOf(30));
        final BigDecimal avgDiscount = BigDecimal.ZERO;
        final Long balanceTid = 123454321L;

        // вставляю запись руками, т.к. кажется бестолково делать это через репозиторий в тесте на репозиторий
        dslContextProvider.ppc(TEST_SHARD)
                .insertInto(CAMPAIGNS_MULTICURRENCY_SUMS)
                .set(CAMPAIGNS_MULTICURRENCY_SUMS.CID, campId)
                .set(CAMPAIGNS_MULTICURRENCY_SUMS.SUM, sum)
                .set(CAMPAIGNS_MULTICURRENCY_SUMS.CHIPS_COST, chipsCost)
                .set(CAMPAIGNS_MULTICURRENCY_SUMS.CHIPS_SPENT, chipsSpent)
                .set(CAMPAIGNS_MULTICURRENCY_SUMS.AVG_DISCOUNT, avgDiscount)
                .set(CAMPAIGNS_MULTICURRENCY_SUMS.BALANCE_TID, balanceTid)
                .execute();

        Map<Long, BigDecimal> chipsCosts = repo.getCampaignsMulticurrencyChipsCosts(TEST_SHARD, List.of(campId));
        assertThat(chipsCosts.keySet(), contains(campId));
        assertThat(chipsCosts.get(campId), comparesEqualTo(chipsCost));
    }

    @Test
    public void getCampaignsMulticurrencyChipsCostsTest_NoRecord() {
        Map<Long, BigDecimal> chipsCosts = repo.getCampaignsMulticurrencyChipsCosts(TEST_SHARD, List.of(campId));
        assertThat(chipsCosts.keySet(), empty());
    }
}
