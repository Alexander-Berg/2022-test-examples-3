package ru.yandex.autotests.direct.cmd.banners.image;


import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Удаление картинки графического баннера из базы в ТГО/РМП кампании")
@Stories(TestFeatures.Banners.DELETE_BANNER)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.DEL_BANNER)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class DeleteImageBannerDBTest {

    private static final String CLIENT = "at-direct-image-banner71";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private ImageBannerRule bannersRule;
    private CampaignTypeEnum campaignType;

    public DeleteImageBannerDBTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
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
    @Description("Удаление записи из images, после удаления баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9225")
    public void deleteImageBannerDBTest() {
        Group saveGroup = bannersRule.getCurrentGroup()
                .withTags(emptyMap());
        if (campaignType == CampaignTypeEnum.TEXT) {
            saveGroup.getBanners().add(BannersFactory.getDefaultTextBanner());
        } else {
            BannersFactory.addNeededAttribute(saveGroup.getBanners().get(0));
            saveGroup.getBanners().add(BannersFactory.getDefaultMobileAppBanner());
        }
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), saveGroup));

        CommonResponse response = cmdRule.cmdSteps().bannerSteps()
                .deleteBanner(String.valueOf(bannersRule.getCampaignId()), String.valueOf(bannersRule.getGroupId()),
                        String.valueOf(bannersRule.getBannerId()), CLIENT);
        assumeThat("удаление прошло успешно", response.getStatus(), equalTo("success"));
        assumeThat("остался один баннер", bannersRule.getCurrentGroup().getBanners(), hasSize(1));

        check();
    }

    @Test
    @Description("Удаление записи из images, после удаления группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9226")
    public void deleteImageBannerGroupDBTest() {
        CommonResponse response = cmdRule.cmdSteps().bannerSteps()
                .deleteBanner(String.valueOf(bannersRule.getCampaignId()), String.valueOf(bannersRule.getGroupId()),
                        String.valueOf(bannersRule.getBannerId()), CLIENT);
        assumeThat("удаление прошло успешно", response.getStatus(), equalTo("success"));
        assumeThat("группа удалилась", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups(), hasSize(0));

        check();
    }

    private void check() {
        ImagesRecord imagesRecord = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).imagesSteps()
                .getImagesRecords(bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
        assertThat("изображение графического баннера удалилось", imagesRecord, nullValue());
    }
}
