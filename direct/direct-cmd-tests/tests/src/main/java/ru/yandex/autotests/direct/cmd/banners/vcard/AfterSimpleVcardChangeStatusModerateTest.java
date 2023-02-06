package ru.yandex.autotests.direct.cmd.banners.vcard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Статус модерации после незначительного изменения визитки через мастер визиток")
@Stories(TestFeatures.Banners.VCARD)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_VCARD)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag("TESTIRT-9435")
@RunWith(Parameterized.class)
public class AfterSimpleVcardChangeStatusModerateTest extends AfterSimpleVcardStatusModerateTestBase {

    private StatusModerate setStatusModerate;
    private StatusModerate expectedStatusModerate;

    public AfterSimpleVcardChangeStatusModerateTest(StatusModerate setStatusModerate, StatusModerate expectedStatusModerate) {
        super(CampaignTypeEnum.TEXT);
        this.setStatusModerate = setStatusModerate;
        this.expectedStatusModerate = expectedStatusModerate;
    }



    @Parameterized.Parameters(name = "statusModerate визитки и баннера после незначительных изменений визитки." +
            " Установленный статус: {0}, Ожидаемый: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {StatusModerate.NO,StatusModerate.READY},
                {StatusModerate.SENT, StatusModerate.READY},
                {StatusModerate.YES, StatusModerate.YES}
        });
    }

    @Override
    protected StatusModerate getSetStatusModerate() {
        return setStatusModerate;
    }

    @Override
    protected StatusModerate getExpectedStatusModerate() {
        return expectedStatusModerate;
    }

    @Override
    protected void sendAndCheck() {
        SaveVCardResponse response = cmdRule.cmdSteps().vCardsSteps()
                .saveVCard(CLIENT, bannersRule.getCampaignId(), bannersRule.getBannerId(),
                        bannersRule.getCurrentGroup().getBanners().get(0).getVcardId(), vcard);
        assumeThat("визитка сохранилась", response.getErrors(), nullValue());
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("статусы соответствуют ожиданию", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10752")
    public void checkStatusModerateEmailChange() {
        super.checkStatusModerateEmailChange();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10753")
    public void checkStatusModerateIMLoginChange() {
        super.checkStatusModerateIMLoginChange();
    }
}
