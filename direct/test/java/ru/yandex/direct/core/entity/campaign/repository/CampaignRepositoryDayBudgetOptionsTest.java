package ru.yandex.direct.core.entity.campaign.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.jooq.Field;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetOptions;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetNotificationStatus;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.SqlUtils;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignRepositoryDayBudgetOptionsTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampaignRepository campaignRepository;

    private int shard;
    private long cid;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        CampaignInfo defaultCampaign = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);
        shard = defaultCampaign.getShard();
        cid = defaultCampaign.getCampaignId();

        LocalDateTime dayBudgetStopTime = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        Campaign campaignDayBudgetOptions = new Campaign()
                .withId(defaultCampaign.getCampaignId())
                .withDayBudgetDailyChangeCount(1L)
                .withDayBudgetNotificationStatus(DayBudgetNotificationStatus.SENT)
                .withDayBudgetStopTime(dayBudgetStopTime);
        campaignRepository.updateDayBudgetOptions(shard, campaignDayBudgetOptions);

        Map<Long, CampaignDayBudgetOptions> dayBudgetCampOptionsMap =
                campaignRepository.getDayBudgetOptions(shard, Arrays.asList(cid));
        assumeThat("проверяем, что в базе сохранился корректный day_budget_stop_time",
                dayBudgetCampOptionsMap.get(cid).getDayBudgetStopTime(), equalTo(dayBudgetStopTime));
    }


    @Test
    public void checkDayBudgetStopTimeResetToZeroTimestamp() {
        campaignRepository.resetDayBudgetStopTimeAndNotificationStatus(shard, Arrays.asList(cid), LocalDateTime.now());

        Field<String> dayBudgetStopTimeCastToString = CAMP_OPTIONS.DAY_BUDGET_STOP_TIME.cast(String.class);
        String stringValue = dslContextProvider.ppc(shard)
                .select(dayBudgetStopTimeCastToString)
                .from(CAMP_OPTIONS)
                .where(CAMP_OPTIONS.CID.eq(cid))
                .fetchOne(dayBudgetStopTimeCastToString);

        assertThat("проверяем, что в базе поле day_budget_stop_time имеет значение: " + SqlUtils.ZERO_TIMESTAMP,
                stringValue, equalTo(SqlUtils.ZERO_TIMESTAMP));
    }
}

