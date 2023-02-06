package ru.yandex.autotests.direct.cmd.groups.mobile;

import java.util.ArrayList;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.GroupsErrorsEnum;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.MobileBannerErrorsEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource.WRONG_INPUT_DATA;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка валидации контроллера saveMobileAdGroups")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
public class SaveMobileAdGroupsValidationTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private Group savingGroup;
    public MobileBannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        savingGroup = bannersRule.getGroup()
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для группы без имени")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9856")
    public void emptyGroupNameTest() {
        savingGroup.setAdGroupName("");
        check(GroupsErrorsEnum.EMPTY_GROUP_NAME.toString());
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для группы без баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9859")
    public void groupWithNoBannersTest() {
        savingGroup.setBanners(new ArrayList<Banner>());
        check(GroupsErrorsEnum.BANNERS_NOT_FOUND.toString());
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для баннера без заголовка")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9860")
    public void bannerWithoutTitleTest() {
        savingGroup.getBanners().get(0).setTitle("");
        check(MobileBannerErrorsEnum.EMPTY_BANNER_TITLE.toString());
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для баннера без ссылки на приложение")
    @TestCaseId("11037")
    public void groupWithoutStoreHrefTest() {
        savingGroup.setStoreContentHref(null);
        check(WRONG_INPUT_DATA.toString());
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для баннера без типа связи")
    @TestCaseId("11038")
    public void groupWithoutNetworkTargeting() {
        savingGroup.setNetworkTargeting(null);
        check(WRONG_INPUT_DATA.toString());
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для баннера без типа устройств")
    @TestCaseId("11039")
    public void groupWithoutNDeviceTypeTargeting() {
        savingGroup.setDeviceTypeTargeting(null);
        check(WRONG_INPUT_DATA.toString());
    }

    private void check(String errorText) {
        ErrorResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroupsInvalidData(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savingGroup));
        assertThat("ошибка соотвествует ожидаемой", errorResponse.getError(), IsEqual.equalTo(errorText));
    }

}
