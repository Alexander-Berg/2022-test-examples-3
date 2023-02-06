package ru.yandex.autotests.direct.cmd.groups.text;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.BannerErrorsEnum;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.GroupsErrorsEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка валидации контроллера saveTextAdGroups")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class SaveTextAdGroupsValidationTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private Group savingGroup;
    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideVCardTemplate(BeanLoadHelper
                    .loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class))
            .withUlogin(CLIENT);

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
    @Description("Проверка ошибки при вызове контроллера без фраз")
    @Ignore("https://st.yandex-team.ru/TESTIRT-8612")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9858")
    public void emptyPhrasesTest() {
        savingGroup.setPhrases(null);
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
        check(BannerErrorsEnum.EMPTY_BANNER_TITLE.toString());
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера для баннера c визиткой без адреса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9861")
    public void bannerWithoutAddressInVcardTest() {
        savingGroup.getBanners().get(0).getContactInfo().setCity("");
        check(GroupsErrorsEnum.EMPTY_ADDRESS.toString());
    }

    private void check(String errorText) {
        ErrorResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savingGroup));
        assertThat("ошибка соотвествует ожидаемой", errorResponse.getError(), IsEqual.equalTo(errorText));
    }

}
