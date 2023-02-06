package ru.yandex.autotests.direct.cmd.groups.dynamic;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение компании с некорректным айди условия нацелевания")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
public class SaveDtoWithIncorrectConditionIdTest extends DtoBaseTest {
    private String incorrectId;

    @Override
    protected Group getDynamicGroup() {
        Group group = super.getDynamicGroup();
        incorrectId = String.valueOf(nextInt(0, Integer.MAX_VALUE));
        group.getDynamicConditions().get(0).setDynId(incorrectId);
        return group;
    }

    @Override
    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        super.createCampaign();
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByCid(campaignId);
    }

    @Test
    @Description("Создаем группу с некорректным айди усоловия динамического нацелевания")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9791")
    public void createGroupWithIncorrectConditionId() {
        savingGroup = getDynamicGroup();

        groupRequest = getGroupRequest();
        groupRequest.setUlogin(CLIENT);
        groupRequest.setCid(campaignId.toString());

        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));
        GroupErrorsResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroupsInvalidData(groupRequest);
        assertThat("Полученная результат соответтвует ожиданиям", errorResponse.getErrors().getGenericErrors().get(0).getText(),
                equalTo(String
                        .format("Ошибка: условие нацеливания №%s не может находится в новой группе",
                                incorrectId
                        )
                )
        );
    }
}
