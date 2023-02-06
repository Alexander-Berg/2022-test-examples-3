package ru.yandex.autotests.direct.cmd.clients;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * TESTIRT-8297
 */
@Aqua.Test
@Description("Сброс statusBsSynced для кампаний пользователя при изменении его настроек (контроллер modifyUser)")
@Stories(TestFeatures.Client.MODIFY_USER)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.MODIFY_USER)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class StatusBsSyncedAfterModifyUserTest {
    private static final String CLIENT = "at-direct-bssynced-client-1";
    private static final String USER_TEMPLATE_1 = "cmd.modifyUser.model.forModifyUserTest-1";
    private static final String USER_TEMPLATE_2 = "cmd.modifyUser.model.forModifyUserTest-2";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public String template;
    @Parameterized.Parameter(1)
    public String change;
    @Parameterized.Parameter(2)
    public String expStatusBsSynced;
    protected TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private ModifyUserModel modifyUserModel;
    private Long campaignId;

    @Parameterized.Parameters(name = "Изначальные настройки {0} пользователя, меняем значение параметра на {1}" +
            " и ожидаем увидеть статус синхронизации с БК {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {USER_TEMPLATE_1, "Yes", StatusBsSynced.NO.toString()},
                {USER_TEMPLATE_1, null, StatusBsSynced.YES.toString()},
//                {USER_TEMPLATE_2, "Yes", StatusBsSynced.YES.toString()},
                {USER_TEMPLATE_2, null, StatusBsSynced.NO.toString()},
        });
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);

        campaignId = bannersRule.getCampaignId();
        modifyUserModel = BeanLoadHelper.loadCmdBean(template, ModifyUserModel.class);
        modifyUserModel.setUlogin(CLIENT);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUserWithVerification(modifyUserModel);

        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(campaignId);
        cmdRule.apiSteps().campaignFakeSteps().setRandomOrderID(campaignId);
        cmdRule.apiSteps().campaignFakeSteps().setBSSynced(campaignId.intValue(), true);
        cmdRule.apiSteps().groupFakeSteps().setGroupFakeStatusBsSynced(bannersRule.getGroupId(), Status.YES);
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannersRule.getBannerId(), Status.YES);
        CampaignsRecord campaign = TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(campaignId);
//        assumeThat("кампания клиента синхронизирована с БК", campaign.getStatusbssynced(),
//                equalTo(CampaignsStatusbssynced.Yes));
    }

    @Test
    @Description("Сброс statusBsSynced для кампании пользователя при изменении настройки - " +
            "\"заблокировать фавиконки на выдаче\"")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9579")
    public void changeIsFaviconBlocked() {
        modifyUserModel.setIsFaviconBlocked(change);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUserWithVerification(modifyUserModel);
        checkStatusBsSynced(expStatusBsSynced);
    }

    @Test
    @Description("Сброс statusBsSynced для кампании пользователя при изменении настройки - " +
            "\"Клиент размещает рекламу Яндекса только в РСЯ\"")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9580")
    public void changeOnlyRSYA() {
        modifyUserModel.setStatusYandexAdv(change);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUserWithVerification(modifyUserModel);
        checkStatusBsSynced(expStatusBsSynced);
    }

    @Test
    @Description("Сброс statusBsSynced для кампании пользователя при изменении настройки - " +
            "\"Клиент размещает рекламу Яндекса только в Яндексе\"")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9581")
    public void changeOnlyYandex() {
        modifyUserModel.setShowOnYandexOnly(change);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUserWithVerification(modifyUserModel);
        checkStatusBsSynced(expStatusBsSynced);
    }

    private void checkStatusBsSynced(String expStatus) {
        ShowCampResponse campResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId.toString());
        assertThat("статус синхронизации кампании с БК соответствует ожидаемому", campResponse.getStatusBsSynced(),
                equalTo(expStatus));
    }


}
