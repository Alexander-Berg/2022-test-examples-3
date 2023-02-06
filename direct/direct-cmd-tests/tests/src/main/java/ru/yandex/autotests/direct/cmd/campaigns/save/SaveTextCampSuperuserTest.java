package ru.yandex.autotests.direct.cmd.campaigns.save;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DeviceTargeting;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Сохранение параметров существующей текстовой кампании, доступных под супером")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveTextCampSuperuserTest {

    public static final String SAVE_TEXT_CAMP_FULL_SUPER = "cmd.saveCamp.request.text.for_SaveTextCampSuperuserTest";
    private final static String CLIENT = "at-direct-backend-c";
    private static TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.
            defaultClassRule().
            useAuth(true).
            as(CLIENT).
            withRules(bannersRule);

    private static SaveCampRequest request;
    private static EditCampResponse response;

    @BeforeClass
    public static void beforeClass() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));

        request = BeanLoadHelper.loadCmdBean(SAVE_TEXT_CAMP_FULL_SUPER, SaveCampRequest.class).
                withCid(String.valueOf(bannersRule.getCampaignId())).
                withUlogin(CLIENT);
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        response = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
    }

    @Test
    @Description("Сохранение таргетинга на устройства существующей текстовой кампании под супером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9506")
    public void testSaveCampDeviceTargeting() {
        DeviceTargeting actual = response.getCampaign().getDeviceTargeting();
        DeviceTargeting expected = DeviceTargeting.fromString(request.getDevice_targeting());
        assertThat("таргетинг на устройства соответствует сохраненному",
                actual, beanDiffer(expected));
    }

    @Test
    @Description("Сохранение списка доменов конкурентов существующей текстовой кампании под супером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9507")
    public void testSaveCampCompetitorsDomains() {
        assertThat("список доменов конкурентов соответствует сохраненному",
                response.getCampaign().getCompetitorsDomains(),
                equalTo(request.getCompetitors_domains()));
    }
}
