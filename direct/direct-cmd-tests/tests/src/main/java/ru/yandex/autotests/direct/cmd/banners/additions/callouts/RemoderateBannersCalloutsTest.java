package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.additions.DeleteBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.RemoderateBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdditionsItemCalloutsStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ModerateAdditionsModerateType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ModerateAdditionsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Перемодерация текстовых дополнений")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.REMODERATE_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class RemoderateBannersCalloutsTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected CalloutsTestHelper helper;
    private String ulogin = "at-direct-banners-callouts-19";
    private String callout;
    private Long cid;

    @Before
    public void setUp() {
        callout = RandomUtils.getString(10);
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
    }

    @After
    public void after() {
        if (cid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(ulogin, cid);
        }
    }

    @Test
    @Description("Перемодерация дополнений для текстовых баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9079")
    public void remoderateCalloutsForTextBanner() {
        saveToTextBanner();
        resetStatusModerateInDb();
        remoderate();
        checkStatusModetrate(AdditionsItemCalloutsStatusmoderate.Ready);
    }

    @Test
    @Description("Принудительная модерация дополнений для текстовых баннеров, проверка статуса модерации дополнений")
    @Ignore("Заработает после мержа https://st.yandex-team.ru/DIRECT-53607")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9081")
    public void acceptModerateCalloutsForTextBanner() {
        saveToTextBanner();
        resetStatusModerateInDb();
        moderateAccept();
        checkStatusModetrate(AdditionsItemCalloutsStatusmoderate.Ready);
    }

    @Test
    @Description("Перемодерация дополнений для динамических баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9080")
    public void remoderateCalloutsForDynamicBanner() {
        saveToDinamicBanner();
        resetStatusModerateInDb();
        remoderate();
        checkStatusModetrate(AdditionsItemCalloutsStatusmoderate.Ready);
    }

    @Test
    @Description("Ручка remoderateBannersAdditions должна добавить запись в moderate_additions с типом pre")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9082")
    public void remoderateShouldAddModerateAdditionsRecord() {
        saveToTextBanner();
        remoderate();
        checkModerateAdditionsRecordsHasType(ModerateAdditionsModerateType.pre);
    }

    @Test
    @Description("Ручка moderateAcceptBannersAdditions должна добавить запись в moderate_additions с типом auto")
    @Ignore("Заработает после мержа https://st.yandex-team.ru/DIRECT-53607")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9083")
    public void moderateAcceptShouldAddModerateAdditionsRecord() {
        saveToTextBanner();
        moderateAccept();
        checkModerateAdditionsRecordsHasType(ModerateAdditionsModerateType.auto);
    }

    private void saveToTextBanner() {
        cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultTextCampaign(ulogin);

        helper.overrideCid(cid.toString());

        helper.saveCallouts(helper.getRequestFor(helper.newGroupAndSet(callout)));
    }

    private void saveToDinamicBanner() {
        cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultDynamicCampaign(ulogin);

        helper.overrideCid(cid.toString());

        helper.saveCalloutsForDynamic(helper.getRequestForDynamic(helper.newDynamicGroupAndSet(callout)));
    }

    private void resetStatusModerateInDb() {
        List<Callout> callouts = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(ulogin).getCallouts();

        assumeThat("Дополнение сохранилось на клиента", callouts, hasSize(1));

        TestEnvironment.newDbSteps().bannerAdditionsSteps().setAdditionsItemCalloutsStatusModerated(
                callouts.get(0).getAdditionsItemId(), AdditionsItemCalloutsStatusmoderate.No);

        List<Banner> groups = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(ulogin, cid.toString()).getGroups();

        assumeThat("Группа сохранилась", groups, hasSize(1));
    }

    private void remoderate() {
        Long adGroupId = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(ulogin, cid.toString()).getGroups().get(0).getAdGroupId();

        DeleteBannersAdditionsResponse response =
                cmdRule.cmdSteps().bannersAdditionsSteps().remoderateClientCallouts(
                        new RemoderateBannersAdditionsRequest()
                                .withAdgroupIds(adGroupId)
                                .withCid(cid)
                                .withUlogin(ulogin)
                );

        assumeThat("Результат запроса перемодерации: 1", response.getSuccess(), equalTo("1"));
    }

    private void moderateAccept() {
        Long adGroupId = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(ulogin, cid.toString()).getGroups().get(0).getAdGroupId();

        DeleteBannersAdditionsResponse response =
                cmdRule.cmdSteps().bannersAdditionsSteps().moderateAcceptClientCallouts(
                        new RemoderateBannersAdditionsRequest()
                                .withAdgroupIds(adGroupId)
                                .withCid(cid)
                                .withUlogin(ulogin)
                );

        assumeThat("Результат запроса перемодерации: 1", response.getSuccess(), equalTo("1"));
    }


    private void checkStatusModetrate(AdditionsItemCalloutsStatusmoderate status) {
        List<Callout> callouts = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(ulogin).getCallouts();

        assumeThat("Дополнение присутствует на клиенте", callouts, hasSize(1));

        assertThat("Статус дополнения - ready", callouts.get(0).getStatusModerate(), equalTo(status));

    }

    private void checkModerateAdditionsRecordsHasType(ModerateAdditionsModerateType type) {
        List<Callout> callouts = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(ulogin).getCallouts();

        List<ModerateAdditionsRecord> rows = TestEnvironment.newDbSteps().bannerAdditionsSteps()
                .getModerateAditions(callouts.get(0).getAdditionsItemId());

        assertThat("Добавлена запись в moderate_additions", rows, hasSize(1));

        assertThat("Статус записи moderate_additions - pre", rows.get(0).getModerateType(),
                equalTo(type));
    }
}
