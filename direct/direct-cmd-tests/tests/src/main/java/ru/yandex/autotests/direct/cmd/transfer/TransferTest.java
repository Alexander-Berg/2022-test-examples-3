package ru.yandex.autotests.direct.cmd.transfer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка переноса средств одного клиента (контроллер transfer)")
@Stories(TestFeatures.Transfer.TRANSFER)
@Features(TestFeatures.TRANSFER)
@Tag(CmdTag.TRANSFER)
@Tag(CampTypeTag.TEXT)
public class TransferTest {
    private static final String CLIENT = "at-direct-backend-c";
    private Long campaignId;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    private BannersRule bannersRule2 = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule, bannersRule2);

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        cmdRule.apiAggregationSteps().activateCampaignWithMoney(CLIENT, bannersRule2.getCampaignId(),
                MoneyCurrency.get(User.get(CLIENT).getCurrency()).getMinServicedInvoiceAmount().floatValue());
    }

    @Test
    @Description("Проверяем, что в списке кампаний для переноса нет архивных")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10046")
    public void noArchiveAtCampaignsFromListTest() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignStopped(campaignId);
        cmdRule.apiAggregationSteps().campaignsArchive(CLIENT, campaignId);

        TransferResponse actualResponse = cmdRule.cmdSteps().transferSteps().getTransfer();
        assumeThat("список не пустой", actualResponse.getCampaignsFromIds(), hasSize(greaterThan(0)));
        assertThat("Список кампаний для переноса не содержит архивных", actualResponse.getCampaignsFromIds(),
                not(hasItems(String.valueOf(campaignId))));
    }

    @Test
    @Description("Проверяем, что в списке кампаний для переноса нет кампаний без денег")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10047")
    public void noWithoutMoneyAtCampaignsFromListTest() {
        cmdRule.apiSteps().makeCampaignModerated(campaignId.intValue());

        TransferResponse actualResponse = cmdRule.cmdSteps().transferSteps().getTransfer();
        assumeThat("список не пустой", actualResponse.getCampaignsFromIds(), hasSize(greaterThan(0)));
        assertThat("Список кампаний для переноса не содержит кампаний без денег", actualResponse.getCampaignsFromIds(),
                not(hasItems(String.valueOf(campaignId))));
    }

    @Test
    @Description("Проверяем, что нельзя переносить деньги клиенту с общим счетом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10045")
    public void noAccountAtCampaignsFromListTest() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.CLIENT_WITH_ACCOUNT));

        TransferResponse actualResponse = cmdRule.cmdSteps().transferSteps().getTransfer();
        assertThat("Список кампаний для переноса пуст", actualResponse.getCampaignsFromIds(), empty());
    }

    @Test
    @Description("Проверяем, что в списке кампаний, на которые переносятся деньги, нет непромодерированных кампаний")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10048")
    public void noWithoutModerateAtCampaignsToListTest() {
        TransferResponse actualResponse = cmdRule.cmdSteps().transferSteps().getTransfer();
        assumeThat("список не пустой", actualResponse.getCampaignsFromIds(), hasSize(greaterThan(0)));
        assertThat("Список кампаний, на которые переносятся деньги, не содержит непромодерированных кампаний",
                actualResponse.getCampaignsToIds(), not(hasItems(String.valueOf(campaignId))));
    }
}
