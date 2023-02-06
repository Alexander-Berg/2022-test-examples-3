package ru.yandex.autotests.direct.cmd.campaigns.absegments;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Валидация сохранения аб-сегментов в кампаниях")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class SaveCampWithAbSegmentsValidationTest {

    private static final String CLIENT = "at-direct-absegment-val";
    private static final Long WRONG_ID = 1L;


    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private SaveCampRequest request;

    @Before
    public void before() {
        Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
        TestEnvironment.newDbSteps(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(clientId);
        request = bannersRule.getSaveCampRequest()
                .withMetrika_counters(MetrikaCountersData.DEFAULT_COUNTER.getCounterId().toString())
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withAbSectionsStat(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId()))
                .withUlogin(CLIENT);
    }

    @Test
    @TestCaseId("11048")
    public void checkSaveInvalidMetrikaCounter() {
        request.withMetrika_counters("");
        saveAndCheck(String.format("Эксперимент №%d не соответсвует счетчикам", MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId()));
    }

    @Test
    @TestCaseId("11049")
    public void checkSaveInvalidAbSectionStat() {
        request.withAbSectionsStat(singletonList(WRONG_ID));
        saveAndCheck(String.format("Эксперимент №%d в архиве", WRONG_ID));
    }

    @Test
    @TestCaseId("11050")
    public void checkSaveInvalidAbSegmentRetargeting() {
        request.withAbSegmentsRetargeting(singletonList(WRONG_ID));
        saveAndCheck(String.format("Сегмент №%d в архиве", WRONG_ID));
    }

    private void saveAndCheck(String error) {
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
        assertThat("Ошибка соответствует ожиданию", response.getCampaignErrors().getError(),
                equalTo(error));
    }
}
