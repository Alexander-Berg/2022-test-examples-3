package ru.yandex.autotests.direct.cmd.banners.image;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Негативные сценарии сохранения графического баннера с banner_type=mobile в РМП кампании")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.MOBILE)
public class MobileAppBannerWithMobileTypeTest {

    private static final String CLIENT = "at-direct-image-banner71";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new ImageBannerRule(CampaignTypeEnum.MOBILE)
            .withImageUploader(new NewImagesUploadHelper())
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Group savingGroup;

    @Before
    public void before() {
        savingGroup = bannersRule.getGroup();
        savingGroup.getBanners().get(0).withBannerType(BannersType.mobile.getLiteral());
    }

    @Test
    @Description("Группа с ГО не создается с banner_type = mobile")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9230")
    public void mobileAppImageBannerWithBannerTypeMobile() {
        cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroups(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savingGroup));

        Long newGroupId = getNewGroupId();
        List<BannersRecord> banners = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannersSteps().getBannerByPid(newGroupId);
        assumeThat("в группе один баннер", banners, hasSize(1));
        assertThat("ошибка соответствует ожиданию", banners.get(0).getBannerType(),
                not(equalTo(BannersType.mobile.getLiteral())));
    }

    private Long getNewGroupId() {
        return Long.valueOf(cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, bannersRule.getCampaignId()).stream()
                .filter(t -> !t.getAdGroupID().equals(String.valueOf(bannersRule.getGroupId())))
                .findFirst().orElseThrow(() -> new DirectCmdStepsException("новая группа не сохранилась"))
                .getAdGroupID());
    }
}
