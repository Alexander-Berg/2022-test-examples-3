package ru.yandex.autotests.direct.cmd.groups.dynamic;

import com.google.gson.Gson;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.dynamicconditions.AjaxEditDynamicConditionsRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Сохранение группы с отсановленным условием")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(CmdTag.EDIT_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
public class SaveDtoGroupWithSuspendedConditionTest extends DtoBaseTest {

    private static final String OGRN = "1027700132195";

    @Override
    protected Group getDynamicGroup() {
        Group expectedGroup = super.getDynamicGroup();
        expectedGroup.getDynamicConditions().clear();
        expectedGroup.getDynamicConditions()
                .add(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_FULL, DynamicCondition.class));
        expectedGroup.getDynamicConditions()
                .add(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_DEFAULT, DynamicCondition.class));
        return expectedGroup;
    }

    @Test
    @Description("Сохранение группы с остановленным условием")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9789")
    public void checkSaveGroup() {
        Group createdGroup = getCreatedGroup().getCampaign().getGroups().get(0);
        DynamicCondition condition = createdGroup.getDynamicConditions().get(0);
        String adGroupId = createdGroup.getAdGroupID();

        suspendCondition(campaignId, condition);
        saveGroup(getCreatedGroup().getCampaign().getGroups().get(0));

        Group actualGroup = getCreatedGroup().getCampaign().getGroups().get(0);
        assertThat("динамическое условие отключено",
                actualGroup.getDynamicConditions().get(0).getIsSuspended(), equalTo("1"));
    }

    private void saveGroup(Group savingGroup) {
        groupRequest = getGroupRequest();
        groupRequest.setUlogin(CLIENT);
        groupRequest.setCid(campaignId.toString());

        savingGroup.setTags(emptyList());
        savingGroup.setHrefParams("");
        savingGroup.getBanners().get(0).withImage("").withHashFlags(null);
        savingGroup.getBanners().get(0).getContactInfo().withOGRN(OGRN);

        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));
        cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroups(groupRequest);
    }

    private void suspendCondition(Long cid, DynamicCondition condition) {
        condition.setIsSuspended("1");
        AjaxEditDynamicConditionsRequest request = AjaxEditDynamicConditionsRequest.fromDynamicCondition(condition)
                .withCid(cid.toString())
                .withUlogin(CLIENT);

        CommonResponse response = cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps().postAjaxEditDynamicConditions(request);
        assumeThat("динамическое условие сохранилось", response.getResult(), equalTo("ok"));
    }
}
