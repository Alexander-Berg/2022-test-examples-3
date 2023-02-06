package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.conditions.DynamicConditionsFactory;
import ru.yandex.autotests.direct.cmd.data.groups.DynamicGroupErrors;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение дто группы с ошибками в условии нацелевания")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Ignore("DIRECT-59019")
public class SaveDynamicGroupConditionNegativeTest {


    protected static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public List<DynamicCondition> conditions;
    public String error;
    private DynamicBannersRule bannersRule;
    private Group savingGroup;

    public SaveDynamicGroupConditionNegativeTest(List<DynamicCondition> conditions, String error) {
        this.conditions = conditions;
        this.error = error;
        bannersRule = new DynamicBannersRule().withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Ошибка - {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                        {
                                DynamicConditionsFactory.duplicateConditions(),
                                DynamicGroupErrors.DUPLICATE_CONDITION.toString(),
                        },
                        {
                                DynamicConditionsFactory.conditionsWithEmptyName(),
                                DynamicGroupErrors.EMPTY_CONDITION_TITLE.toString()
                        },
                        {
                                DynamicConditionsFactory.conditionWithExtraLowPrice(),
                                DynamicGroupErrors.SEARCH_PRICE_CAN_NOT_BE_LOWER_THAN.toString()
                        },
                        {
                                DynamicConditionsFactory.maxConditionsInGroup(),
                                DynamicGroupErrors.MAX_CONDITIONS_IN_GROUP.toString()
                        },
                }
        );
    }

    @Before
    public void before() {
        savingGroup = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_DEFAULT2, Group.class);
        savingGroup.setDynamicConditions(conditions);
    }

    @Test
    @Description("Невозможность создания дто группы с невалидным условием")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9793")
    public void checkAddDynamicFeedGroupWithCond() {
        GroupErrorsResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroupsInvalidData(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savingGroup));
        assertThat("ошибка совпадает с ожидаемой", errorResponse.getErrors().getGroupErrors().getArrayErrors()
                        .get(0).getObjectErrors().getDinamicConditions().getArrayErrors().get(0).getDescription(),
                equalTo(error)
        );
    }

    @Test
    @Description("Несохранение изменения дто группы с невалидным условием")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9792")
    public void checkSaveDynamicFeedGroupWithCond() {
        savingGroup.withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        savingGroup.getDynamicConditions().get(0).withAdGroupId(bannersRule.getGroupId());

        GroupErrorsResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroupsInvalidData(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savingGroup));
        assertThat("ошибка совпадает с ожидаемой", errorResponse.getErrors().getGroupErrors().getArrayErrors()
                        .get(0).getObjectErrors().getDinamicConditions().getArrayErrors().get(0).getDescription(),
                equalTo(error)
        );
    }
}
