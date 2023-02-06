package ru.yandex.autotests.direct.cmd.groups.dynamic;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Несохранение параметров ссылки дто")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
public class NegativeSaveDtoBannerDomainParamsTest extends DtoBaseTest {

    @Parameterized.Parameter(0)
    public String hrefParams;

    @Parameterized.Parameter(1)
    public String error;

    private GroupErrorsResponse response;

    @Parameterized.Parameters(name = "Параметры : {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {random(2025, '1'), "Превышена допустимая длина"},
        });
    }

    @Test
    @Description("Несохранение параметров ссылки дто")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9786")
    public void checkHrefParams() {
        assertThat("ошибка совпадает с ожидаемой", response.getErrors().getGroupErrors().getArrayErrors().get(0)
                .getObjectErrors().getHrefParams().get(0).getText(), equalTo(error));
    }

    @Override
    protected void createGroupAndGetIds() {
        createGroup();
    }

    @Override
    protected void createGroup() {
        savingGroup = getDynamicGroup();

        groupRequest = getGroupRequest();
        groupRequest.setUlogin(CLIENT);
        groupRequest.setCid(campaignId.toString());

        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));
        response = cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroupsInvalidData(groupRequest);
    }

    @Override
    protected Group getDynamicGroup() {
        Group group = super.getDynamicGroup();
        group.setHrefParams(hrefParams);
        return group;
    }
}
