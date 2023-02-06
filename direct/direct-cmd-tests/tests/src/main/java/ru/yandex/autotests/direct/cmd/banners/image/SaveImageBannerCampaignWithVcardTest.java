package ru.yandex.autotests.direct.cmd.banners.image;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.commons.banner.OrgDetails;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyMap;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Единая визитка при сохранении графического баннера в ТГО кампании")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveImageBannerCampaignWithVcardTest {

    private static final String CLIENT = "at-direct-image-banner71";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private ContactInfo vcard = BeanLoadHelper
            .loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);
    private BannersRule bannersRule = new TextBannersRule()
            .overrideVCardTemplate(vcard)
            .overrideCampTemplate(SaveCampRequest.fillContactInfo(vcard).withCamp_with_common_ci("1"))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private DirectCmdSteps directCmdSteps;
    private NewImagesUploadHelper imagesUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper()
            .withBannerImageSteps(
                    cmdRule.cmdSteps().bannerImagesSteps()
            ).withClient(CLIENT);
    private Long campaignId;
    private Banner saveBanner;

    @Before
    public void before() {
        directCmdSteps = cmdRule.cmdSteps();
        campaignId = bannersRule.getCampaignId();
        imagesUploadHelper.upload();
        saveBanner = BannersFactory.getDefaultTextImageBanner()
                .withImageAd(new ImageAd().withHash(imagesUploadHelper.getUploadResponse().getHash()))
                .withContactInfo(vcard)
                .withHasVcard(1);
    }

    @Test
    @Description("Единая визитка после добавления новой группы с графическим баннером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9247")
    public void addGroupWithImageBanner() {
        Group expectedGroup = bannersRule.getGroup();
        expectedGroup.getBanners().clear();
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        check();
    }

    @Test
    @Description("Единая визитка после добавления новой группы с текстовым и графическим баннером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9248")
    public void addGroupWithTextAndImageBanner() {
        Group expectedGroup = bannersRule.getGroup();
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        check();
    }

    @Test
    @Description("Единая визитка после добавление графического баннера в существующую группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9249")
    public void addImageBannerToGroup() {
        Group expectedGroup = directCmdSteps.groupsSteps().getGroup(CLIENT, campaignId, bannersRule.getGroupId())
                .withTags(emptyMap());
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        check();
    }

    private void saveGroup(Group expectedGroup) {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, expectedGroup));
    }

    private void check() {
        EditCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(campaignId, CLIENT);
        prepareVcard();
        assertThat("единая визитка не изменилась", actualResponse.getCampaign().getVcard(),
                beanDiffer(vcard).useCompareStrategy(onlyExpectedFields()));
    }

    private void prepareVcard() {
        vcard.setOrgDetails(new OrgDetails(null, vcard.getOGRN()));
        vcard.setOGRN(null);
        vcard.setAutoBound(null);
        vcard.setAutoPoint(null);
    }
}
