package ru.yandex.autotests.direct.cmd.banners.vcard;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.vcards.SaveVCardResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("статус модерации после Значительного изменения визитки через мастер визиток")
@Stories(TestFeatures.Banners.VCARD)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_VCARD)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag("TESTIRT-9435")
public class AfterVcardChangeStatusModerateYesTest extends AfterVcardStatusModerateTestBase {

    public AfterVcardChangeStatusModerateYesTest() {
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

    @Override
    protected void sendAndCheck() {
        SaveVCardResponse response = cmdRule.cmdSteps().vCardsSteps()
                .saveVCard(CLIENT, bannersRule.getCampaignId(), bannersRule.getBannerId(),
                bannersRule.getCurrentGroup().getBanners().get(0).getVcardId(), vcard);
        assumeThat("визитка сохранилась", response.getErrors(), nullValue());
        Group actualGroup = bannersRule.getCurrentGroup();
        prepareActualGroup(actualGroup);
        assertThat("статусы соответствуют ожиданию", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10778")
    public void checkStatusModerateApartChange() {
        super.checkStatusModerateApartChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10779")
    public void checkStatusModerateHouseChange() {
        super.checkStatusModerateHouseChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10780")
    public void checkStatusModerateBuildChange() {
        super.checkStatusModerateBuildChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10781")
    public void checkStatusModerateOGRNChange() {
        super.checkStatusModerateOGRNChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10782")
    public void checkStatusModerateCountryChange() {
        super.checkStatusModerateCountryChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10783")
    public void checkStatusModerateCityChange() {
        super.checkStatusModerateCityChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10784")
    public void checkStatusModerateCompanyNameChange() {
        super.checkStatusModerateCompanyNameChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10785")
    public void checkStatusModerateExtraMessageChange() {
        super.checkStatusModerateExtraMessageChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10786")
    public void checkStatusModerateStreetChange() {
        super.checkStatusModerateStreetChange();
    }
}
