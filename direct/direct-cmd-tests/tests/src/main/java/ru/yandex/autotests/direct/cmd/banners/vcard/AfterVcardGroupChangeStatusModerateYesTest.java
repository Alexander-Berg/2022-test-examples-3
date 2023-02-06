package ru.yandex.autotests.direct.cmd.banners.vcard;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("статус модерации после Значительного изменения визитки через группу")
@Stories(TestFeatures.Banners.VCARD)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_VCARD)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag("TESTIRT-9435")
public class AfterVcardGroupChangeStatusModerateYesTest extends AfterVcardStatusModerateTestBase {

    private Group expectedGroup;

    public AfterVcardGroupChangeStatusModerateYesTest() {
        super(CampaignTypeEnum.TEXT);
    }

    @Override
    protected StatusModerate getSetStatusModerate() {
        return StatusModerate.YES;
    }

    @Override
    protected StatusModerate getExpectedStatusModerate() {
        return StatusModerate.READY;
    }

    @Before
    @Override
    public void before() {
        super.before();
        expectedGroup = getGroup();
        vcard = expectedGroup.getBanners().get(0).getContactInfo();
    }

    @Override
    protected void sendAndCheck() {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT,
                bannersRule.getCampaignId(), expectedGroup));
        Group actualGroup = bannersRule.getCurrentGroup();
        prepareActualGroup(actualGroup);
        assertThat("статусы соответствуют ожиданию", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10805")
    public void checkStatusModerateApartChange() {
        super.checkStatusModerateApartChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10806")
    public void checkStatusModerateHouseChange() {
        super.checkStatusModerateHouseChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10807")
    public void checkStatusModerateBuildChange() {
        super.checkStatusModerateBuildChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10808")
    public void checkStatusModerateOGRNChange() {
        super.checkStatusModerateOGRNChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10809")
    public void checkStatusModerateCountryChange() {
        super.checkStatusModerateCountryChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10810")
    public void checkStatusModerateCityChange() {
        super.checkStatusModerateCityChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10811")
    public void checkStatusModerateCompanyNameChange() {
        super.checkStatusModerateCompanyNameChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10812")
    public void checkStatusModerateExtraMessageChange() {
        super.checkStatusModerateExtraMessageChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10813")
    public void checkStatusModerateStreetChange() {
        super.checkStatusModerateStreetChange();
    }
}
