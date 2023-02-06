package ru.yandex.autotests.direct.httpclient.banners.editgroups.showcampmultiedit;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
@Aqua.Test
@Description("Проверка контроллера showCampMultiEdit при редактировнии нескольких групп с баннерами")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
public class ExistedMultipleGroupsShowCampMultiEditTest extends ShowCampMultiEditTestBase {

    public void init(){
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, bannersRule.getGroup()));
        Group secondGroup = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, bannersRule.getCampaignId())
                .stream().filter(group -> !bannersRule.getGroupId().toString().equals(group.getAdGroupID()))
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось, что есть вторая группа"));
        expectedGroups = new PropertyLoader<>(GroupsCmdBean.class).getHttpBean("multipleGroupForShowCampMultiEdit");
        requestParams.setAdgroupIds(String.valueOf(StringUtils.join(bannersRule.getGroupId(), ",", secondGroup.getAdGroupID())));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10148")
    public void showCampMultiEditResponseTest() {
        super.showCampMultiEditResponseTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10147")
    public void showCampMultiEditLightResponseTest() {
        super.showCampMultiEditLightResponseTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10149")
    public void goBackShowCampMultiEditResponseTest() {
        super.goBackShowCampMultiEditResponseTest();
    }
}
