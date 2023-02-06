package ru.yandex.autotests.direct.httpclient.banners.editgroups.savetextadgroups;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.GroupsErrorsEnum;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 12.05.15.
 * TESTIRT-4953
 */
@Aqua.Test
@Description("Проверка контроллера saveTextAdGroups при сохранении группы с максимальным количеством баннеров")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CampTypeTag.TEXT)
public class MaxBannersCountSaveTextAdGroupsTest {
    private static final String CLIENT_LOGIN = "at-direct-b-bannersmultisave";

    BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static CSRFToken csrfToken;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    protected GroupsParameters requestParams;
    protected GroupsCmdBean groupsForSave;

    @Before
    public void before() {
        requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);

        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());

        requestParams.setCid(String.valueOf(bannersRule.getCampaignId()));
        requestParams.setAdgroupIds("0");

    }

    @Test
    @Description("Сохранение группы с максимальным количеством баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10126")
    public void maxBannersInGroupTest() {
        groupsForSave = new PropertyLoader<>(GroupsCmdBean.class).getHttpBean("groupWithMaxBannersCount2");
        requestParams.setJsonGroups(groupsForSave.toJson());
        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        List<Group> groups =
                cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT_LOGIN, bannersRule.getCampaignId());
        assertThat("количество сохраненных баннеров соответствует ожиданиям",
                groups.get(groups.size() - 1).getBanners(), hasSize(50));
    }

    @Test
    @Description("Проверка ошибки при сохранении группы с количеством баннеров, больше допустимого")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10125")
    public void bannersInGroupCountAboveMaxTest() {
        groupsForSave = new PropertyLoader<>(GroupsCmdBean.class).getHttpBean("groupWithBannerCountAboveMax2");
        requestParams.setJsonGroups(groupsForSave.toJson());
        DirectResponse response = cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseError(response, equalTo(GroupsErrorsEnum.TOO_MANY_BANNERS.toString()));
        List<Banner> banners =
                cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT_LOGIN, bannersRule.getCampaignId());
        assertThat("группа не сохранилась", banners,
                hasSize(1));
    }
}
