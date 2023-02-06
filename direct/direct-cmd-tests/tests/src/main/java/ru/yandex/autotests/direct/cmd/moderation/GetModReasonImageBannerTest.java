package ru.yandex.autotests.direct.cmd.moderation;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showdiag.ShowDiagResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ImagesStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ModReasonsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ImagesRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Получение причин отклонения на картиночный баннер")
@Stories(TestFeatures.ShowDiag.GET_SHOW_DIAG)
@Features(TestFeatures.SHOW_DIAG)
@Tag(CmdTag.SHOW_DIAG)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class GetModReasonImageBannerTest {
    private static final String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private DirectJooqDbSteps dbSteps;
    private BannersRule bannerRule;

    public GetModReasonImageBannerTest(CampaignTypeEnum campaignType) {
        bannerRule = new ImageBannerRule(campaignType)
                .withImageUploader(new NewImagesUploadHelper())
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
    }

    @Parameterized.Parameters(name = "Получение причин отклонения на картиночный баннер. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        dbSteps = TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
    }

    private void createModReasonForImage() {
        ImagesRecord imagesRecord = dbSteps.imagesSteps().getImagesRecords(
                bannerRule.getCampaignId(),
                bannerRule.getGroupId(),
                bannerRule.getBannerId());

        dbSteps.showDiagSteps().createModReasonsRecord(
                ModReasonsType.image_ad,
                imagesRecord.getImageId(),
                "--- -   id: 304");
        dbSteps.imagesSteps().setImageStatusModerate(
                bannerRule.getBannerId(),
                bannerRule.getBanner().getImageAd().getHash(),
                ImagesStatusmoderate.No);
    }

    private void createModReasonForBanner() {
        dbSteps.showDiagSteps().createModReasonsRecord(
                ModReasonsType.banner,
                bannerRule.getBannerId(),
                "--- -   id: 306");
        dbSteps.bannersSteps().setBannerStatusModerate(
                bannerRule.getBannerId(),
                BannersStatusmoderate.No);
    }

    @Test
    @Description("Получение причин отклонения на графический баннер")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9913")
    public void getModReasonForBannerOnly() {
        createModReasonForBanner();
        ShowDiagResponse response = cmdRule.cmdSteps().showDiagSteps().getShowDiag(bannerRule.getBannerId().toString());
        assertThat("получили 1 причину отклонения", response.getBannerDiags().getBanner(), hasSize(1));
    }

    @Test
    @Description("Получение причин отклонения на картинку графического баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9914")
    public void getModReasonForImage() {
        createModReasonForImage();
        ShowDiagResponse response = cmdRule.cmdSteps().showDiagSteps().getShowDiag(bannerRule.getBannerId().toString());
        assertThat("получили 1 причину отклонения", response.getBannerDiags().getBanner(), hasSize(1));
    }

    @Test
    @Description("Получение причин отклонения на графический баннер и картинку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9915")
    public void getModReasonForBannerAndImage() {
        createModReasonForBanner();
        createModReasonForImage();
        ShowDiagResponse response = cmdRule.cmdSteps().showDiagSteps().getShowDiag(bannerRule.getBannerId().toString());
        assertThat("получили 2 причины отклонения", response.getBannerDiags().getBanner(), hasSize(2));
    }

}
