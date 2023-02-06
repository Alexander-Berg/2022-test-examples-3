package ru.yandex.autotests.direct.cmd.campaigns.editcamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.OrgDetails;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка контроллера editCamp")
@Stories(TestFeatures.Campaigns.EDIT_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.EDIT_CAMP)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class EditCampTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private ContactInfo expectedContactInfo =
            BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);

    private BannersRule bannersRule = new TextBannersRule()
            .overrideVCardTemplate(expectedContactInfo)
            .overrideCampTemplate(SaveCampRequest.fillContactInfo(expectedContactInfo).withCamp_with_common_ci("1"))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
    }

    @Test
    @Description("Проверяем единую контактную информацию в ответе контроллера editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10339")
    public void editCampContactInfoTest() {
        EditCampResponse editCampResponse =
                cmdRule.cmdSteps().campaignSteps().getEditCamp(campaignId, CLIENT);
        prepareContactInfo();
        assertThat("Блок vcard в ответе контроллера совпадает с ожидаемым",
                editCampResponse.getCampaign().getVcard(),
                beanDiffer(expectedContactInfo).useCompareStrategy(onlyExpectedFields()));
    }

    private void prepareContactInfo() {
        expectedContactInfo.setOrgDetails(new OrgDetails(null, expectedContactInfo.getOGRN()));
        expectedContactInfo.setOGRN(null);
        expectedContactInfo.setAutoBound(null);
        expectedContactInfo.setAutoPoint(null);
    }
}
