package ru.yandex.autotests.direct.cmd.campaigns.delete;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ImagesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.runDeleteCampaignScriptAndCheckResult;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Удаление картинки графического баннера при удалении ТГО/РМП кампании")
@Stories(TestFeatures.Campaigns.DEL_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.DEL_BANNER)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class DeleteCampaignWithImageBannerDBTest {

    private static final String CLIENT = "at-direct-image-banner71";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private ImageBannerRule bannersRule;

    public DeleteCampaignWithImageBannerDBTest(CampaignTypeEnum campaignType) {
        bannersRule = new ImageBannerRule(campaignType)
                .withImageUploader(new NewImagesUploadHelper())
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Удаление графического объявления. Тип кампании {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Test
    @Description("Удаление записи из images, после удаления кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9405")
    public void deleteImageBannerCampaignDBTest() {
        cmdRule.cmdSteps().campaignSteps()
                .deleteCampaign(CLIENT, bannersRule.getCampaignId());
        runDeleteCampaignScriptAndCheckResult(cmdRule, Long.parseLong(User.get(CLIENT).getClientID()),
                bannersRule.getCampaignId());
        ImagesRecord imagesRecord = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).imagesSteps()
                .getImagesRecords(bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
        assertThat("изображение графического баннера удалилось", imagesRecord, nullValue());
    }

}
