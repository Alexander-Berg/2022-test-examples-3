package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class MobileContentCampaignWithCustomStrategyAddOperationSupportTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private MobileContentCampaignWithCustomStrategyAddOperationSupport testingSupport;

    private static final long GOAL_ID = 55L;

    private static RestrictedCampaignsAddOperationContainer addContainer;

    @BeforeClass
    public static void beforeClass() {
        addContainer = RestrictedCampaignsAddOperationContainer.create(RandomNumberUtils.nextPositiveInteger(),
                RandomNumberUtils.nextPositiveLong(), ClientId.fromLong(RandomNumberUtils.nextPositiveLong()),
                RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong());
    }

    public static Object[][] parametersOfAddingCampaignWithCpiStrategy() {
        return new Object[][]{
                {CampaignConstants.DEFAULT_CPI_GOAL_ID, null},
                {GOAL_ID, GOAL_ID},
                {null, null},
        };
    }

    @Test
    @TestCaseName("отправленная цель {0}, ожидаем цель {1}")
    @Parameters(method = "parametersOfAddingCampaignWithCpiStrategy")
    public void addCampaignWithCpiStrategy(Long goalId, Long expectedGoalId) {
        MobileContentCampaignWithCustomStrategy campaign = new MobileContentCampaign()
                .withStrategy(averageCpiStrategy(goalId));

        testingSupport.onPreValidated(addContainer, List.of(campaign));

        assertThat(campaign.getStrategy().getStrategyData().getGoalId())
                .as("цель в кампании")
                .isEqualTo(expectedGoalId);
    }
}
