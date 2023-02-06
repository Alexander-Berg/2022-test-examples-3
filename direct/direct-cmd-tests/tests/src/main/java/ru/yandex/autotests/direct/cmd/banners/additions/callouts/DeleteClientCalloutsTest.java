package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdditionsItemCalloutsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Удаление дополнений")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.DELETE_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class DeleteClientCalloutsTest {

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin = "at-direct-banners-callouts-16";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(Logins.SUPER);
    CalloutsTestHelper helper;
    private String callout = "callout1";
    private Callout savedCallout;

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
        List<Callout> callouts = cmdRule.cmdSteps()
                .bannersAdditionsSteps().saveBannersCalloutsList(ulogin, callout);
        savedCallout = callouts.get(0);
    }

    @Test
    @Description("Удаленное дополнение при повторном сохранении не должно дублироваться(id должен быть прежним)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9073")
    public void addDeletedCalloutShouldHavePreviousId() {
        cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsSafe(ulogin, savedCallout);

        cmdRule.cmdSteps()
                .bannersAdditionsSteps().saveBannersCalloutsList(ulogin, callout);

        List<Callout> actualCallouts = cmdRule.cmdSteps().bannersAdditionsSteps()
                .getCallouts(ulogin).getCallouts();

        assumeThat("Сохранено 1но дополнение", actualCallouts, hasSize(1));

        assertThat("id сохраненного дополнения совпадает с удаленным", actualCallouts.get(0), beanDiffer(savedCallout));
    }

    @Test
    @Description("Должен выставляться флаг is_deleted для дополнений при удалении")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9074")
    public void deleteShouldSetIsDeletedFlag() {
        cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsSafe(ulogin, savedCallout);

        List<AdditionsItemCalloutsRecord> callouts = TestEnvironment.newDbSteps().useShardForLogin(ulogin)
                .bannerAdditionsSteps().getClientCallouts(Long.valueOf(User.get(ulogin).getClientID()));

        assertThat("для клиента в базе только одно дополнение", callouts, hasSize(1));

        assertThat("выставлени флаг is_deleted у дополнения", callouts.get(0).getIsDeleted(),
                equalTo(1));
    }

    @Test
    @Description("Удаление дополнений с отвязкой от текстовых баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9075")
    public void deleteCalloutsAndUnlinkFromTextBanner() {
        String cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultTextCampaign(ulogin).toString();

        helper.overrideCid(cid);

        GroupsParameters request = helper.getRequestFor(helper.newGroupAndSet(callout));
        helper.saveCallouts(request);

        cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsSafe(ulogin, savedCallout);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(ulogin, cid);

        List<String> actualCallouts = helper.getCalloutsList(response);

        assertThat("дополнения удалились с баннера", actualCallouts, hasSize(0));
    }


    @Test
    @Description("Удаление дополнений с отвязкой от динамических баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9076")
    public void deleteCalloutsAndUnlinkFromDynamicBanner() {
        String cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultDynamicCampaign(ulogin).toString();

        helper.overrideCid(cid);

        GroupsParameters request = helper.getRequestForDynamic(helper.newDynamicGroupAndSet(callout));
        helper.saveCalloutsForDynamic(request);

        cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsSafe(ulogin, savedCallout);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(ulogin, cid);

        List<String> actualCallouts = helper.getCalloutsList(response);

        assertThat("дополнения удалились с баннера", actualCallouts, hasSize(0));
    }


}
