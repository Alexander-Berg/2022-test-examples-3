package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.DynamicGroupsErrorTexts;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение дто группы с некорректными условиями нацелевания")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
@Ignore("DIRECT-59019")
public class SaveDynamicGroupsValidationTest {
    protected static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private DynamicBannersRule bannersRule;
    private GroupsParameters groupRequest;

    public SaveDynamicGroupsValidationTest() {
        bannersRule = new DynamicBannersRule().withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }


    public GroupErrorsResponse createGroup(Group savingGroup) {
        Group group = savingGroup;
        group.setCampaignID(String.valueOf(bannersRule.getCampaignId()));
        group.getBanners().stream().forEach(b -> b.withCid(bannersRule.getCampaignId()));
        groupRequest = GroupsParameters.forNewCamp(CLIENT, bannersRule.getCampaignId(), savingGroup);
        return cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroupsInvalidData(groupRequest);
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без имени группы ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9799")
    public void emptyGroupNameTest() {
        GroupErrorsResponse response = createGroup(GroupsFactory.getCommonDynamicGroup().withAdGroupName(""));
        assertThat("ошибка соответствует ожиданиям", response.getErrors().getGroupErrors().getArrayErrors().get(0)
                        .getObjectErrors().getAdgroupNameErrors().get(0).getDescription(),
                equalTo(DynamicGroupsErrorTexts.NEED_GROUP_NAME.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без баннеров ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9800")
    public void groupWithoutBannersTest() {
        GroupErrorsResponse response = createGroup(GroupsFactory.getCommonDynamicGroup().withBanners(new ArrayList<>()));
        assertThat("ошибка соответствует ожиданиям", response.getErrors().getGroupErrors().getArrayErrors().get(0)
                        .getObjectErrors().getAdgroupNameErrors().get(0).getDescription(),
                equalTo(DynamicGroupsErrorTexts.ERROR_BANNERS_NOT_FOUND.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы для баннера без текста ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9798")
    public void bannerWithoutBodyTest() {
        Group group = GroupsFactory.getCommonDynamicGroup();
        group.getBanners().get(0).withBody("");
        GroupErrorsResponse response = createGroup(group);
        assertThat("ошибка соответствует ожиданиям", response.getErrors().getGroupErrors().getArrayErrors().get(0)
                        .getObjectErrors().getBannersErrors().getArrayErrors().get(0).getObjectErrors().getBody().get(0)
                        .getDescription(),
                equalTo(DynamicGroupsErrorTexts.ERROR_BANNER_BODY_NOT_FOUND.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без условий нацеливания ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9801")
    public void bannerWithoutConditionsTest() {
        GroupErrorsResponse response = createGroup(GroupsFactory.getCommonDynamicGroup().withDynamicConditions(new ArrayList<>()));
        assertThat("ошибка соответствует ожиданиям", response.getErrors().getDinamicConditions().getArrayErrors().get(0).getDescription(),
                equalTo(DynamicGroupsErrorTexts.ERROR_DYNAMIC_CONDITIONS_NOT_FOUND.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без домена")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9802")
    public void bannerWithoutMainDomainTest() {
        GroupErrorsResponse response = createGroup(GroupsFactory.getCommonDynamicGroup().withMainDomain(""));
        assertThat("ошибка соответствует ожиданиям", response.getErrors().getGroupErrors().getArrayErrors().get(0)
                        .getObjectErrors().getMainDomain().get(0).getDescription(),
                equalTo(DynamicGroupsErrorTexts.NEED_MAIN_DOMAIN.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без параметров url")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9803")
    public void bannerWithoutHrefParamsTest() {
        GroupErrorsResponse response = createGroup(GroupsFactory.getCommonDynamicGroup().withHrefParams(null));
        assertThat("ошибка соответствует ожиданиям", response.getErrors().getGroupErrors().getGenericErrors().get(0)
                        .getText(),
                equalTo(DynamicGroupsErrorTexts.BAD_INPUT_PARAMS.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы c 1025 символами в параметрах url")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9804")
    public void bannerWithBadHrefParamsTest() {
        GroupErrorsResponse response = createGroup(GroupsFactory.getCommonDynamicGroup().withHrefParams(RandomStringUtils.randomAlphabetic(1025)));
        assertThat("ошибка соответствует ожиданиям", response.getErrors().getGroupErrors().getArrayErrors().get(0)
                        .getObjectErrors().getHrefParams().get(0).getDescription(),
                equalTo(DynamicGroupsErrorTexts.ERROR_HREF_PARAMS.toString()));
    }


}
