package ru.yandex.autotests.direct.cmd.clients.greenurl;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BsResyncQueueRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Транспорт баннера в БК при редактировании " +
        "настройки пользователя no_display_hrefs (cmd = modifyUser)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.MODIFY_USER)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class NoDisplayHrefsParamAtModifyUserTransportTest {

    private static final String CLIENT = "at-direct-backend-modifyuser-6";
    private static final String DISPLAY_HREF = "somehref";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public String displayHref;
    @Parameterized.Parameter(1)
    public String noDisplayHrefsResetValue;
    @Parameterized.Parameter(2)
    public String noDisplayHrefsNewValue;
    private CampaignRule campaignRule = new CampaignRule().
            withMediaType(CampaignTypeEnum.TEXT).
            withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);

    private Long groupId;
    private Long bannerId;

    @Parameterized.Parameters(name = "banner.display_href = \"{0}\", " +
            "устанавливаем no_display_hrefs в \"{2}\"")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // баннер с отображаемой ссылкой, меняем настройку и ожидаем что баннер переотправится
                {DISPLAY_HREF, "0", "1"},
                {DISPLAY_HREF, "1", "0"},
                // баннер с отображаемой ссылкой, не меняем настройку и ожидаем что баннер не переотправится
                {DISPLAY_HREF, "0", "0"},
                {DISPLAY_HREF, "1", "1"},
                // баннер без отображаемой ссылки, меняем настройку и ожидаем что баннер не переотправится
                {null, "0", "1"},
                {null, "1", "0"}
        });
    }

    @Before
    public void before() {
        setOptionWithAssumption(noDisplayHrefsResetValue);
        createGroup();
        getCreatedIds();
        assumeDisplayHref();
        makeAllModerated();
        makeAllBsSynced();
    }

    @Test
    @Description("Транспорт баннера в БК при редактировании " +
            "настройки пользователя no_display_hrefs (cmd = modifyUser)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9591")
    public void testNoDisplayHrefsParamAtUserSettingsTransport() {
        setOptionWithAssumption(noDisplayHrefsNewValue);

        BsResyncQueueRecord resyncEntry = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bsResyncQueueSteps()
                .getBsResyncQueueRecord(campaignRule.getCampaignId(), 0L, bannerId);
        Matcher matcher = displayHref != null && !noDisplayHrefsResetValue.equals(noDisplayHrefsNewValue) ?
                notNullValue() : nullValue();
        assertThat("запись в ppc.bs_resync_queue соответствует ожидаемой", resyncEntry, matcher);
    }

    private void createGroup() {
        Group group = GroupsFactory.getDefaultTextGroup();

        Long campaignId = campaignRule.getCampaignId();
        group.setCampaignID(campaignId.toString());
        group.getBanners().stream().
                forEach(b -> b.withCid(campaignId).withDisplayHref(displayHref));

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(
                CLIENT, campaignId, group);

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupRequest);
    }

    private void getCreatedIds() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT, campaignRule.getCampaignId().toString());
        groupId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"))
                .getAdGroupId();
        bannerId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть баннер"))
                .getBid();
    }

    private void assumeDisplayHref() {
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(
                CLIENT, campaignRule.getCampaignId(), groupId, bannerId);
        String actualDisplayHref = response.getCampaign().
                getGroups().get(0).getBanners().get(0).getDisplayHref();
        assumeThat("параметр dislay_href сохранился в баннере при создании группы",
                actualDisplayHref, equalTo(displayHref));
    }

    private void makeAllModerated() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(campaignRule.getCampaignId());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(groupId);
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerId);
    }

    private void makeAllBsSynced() {
        cmdRule.apiSteps().campaignFakeSteps().setBSSynced((int) (long) campaignRule.getCampaignId(), true);
        cmdRule.apiSteps().groupFakeSteps().setGroupFakeStatusBsSynced(groupId, Status.YES);
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES);
    }

    private void setOptionWithAssumption(String value) {
        ModifyUserModel modifyUserModel = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT);
        modifyUserModel.withPhone("+79999999999").withNoDisplayHrefs(value).withUlogin(CLIENT);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(modifyUserModel);
        String actual = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT).getNoDisplayHrefs();
        assumeThat("настройка пользователя no_display_hrefs выставлена", actual, getExpectedMatcher(value));
    }

    private Matcher getExpectedMatcher(String value) {
        return "".equals(value) || "0".equals(value) ? anyOf(nullValue(), equalTo("0")) : equalTo("1");
    }
}
