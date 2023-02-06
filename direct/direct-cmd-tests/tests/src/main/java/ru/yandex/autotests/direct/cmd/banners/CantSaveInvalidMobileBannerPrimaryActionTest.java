package ru.yandex.autotests.direct.cmd.banners;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.banners.MobileBannerPrimaryAction;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение названия действия для баннера рмп")
@Stories(TestFeatures.Banners.MOBILE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag("DIRECT-55077")
public class CantSaveInvalidMobileBannerPrimaryActionTest {

    public static final String CLIENT = "at-direct-backend-b";

    private MobileBannerPrimaryAction action = MobileBannerPrimaryAction.INVALID;

    private CampaignRule campaignRule = new CampaignRule()
            .withMediaType(CampaignTypeEnum.MOBILE)
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);

    @Test
    @Description("Возможно сохранить название действия")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9054")
    public void canSaveBannerPrimaryAction() {
        Group group = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2, Group.class);

        group.setCampaignID(campaignRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> {
            b.withAdType("text");
            b.withCid(campaignRule.getCampaignId());
        });
        group.getBanners().get(0).setPrimaryAction(action);

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);
        ErrorResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroupsInvalidData(groupRequest);

        assertThat("не удалось сохранить невалидный primary action ", errorResponse.getError(),
                equalTo(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }


}
