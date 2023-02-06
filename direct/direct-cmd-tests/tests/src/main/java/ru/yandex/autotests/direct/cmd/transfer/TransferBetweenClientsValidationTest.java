package ru.yandex.autotests.direct.cmd.transfer;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsCustomOptionsRecord;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.transfer.TransferErrors;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка ошибок страницы переноса средств между клиентами (контроллер transfer)")
@Stories(TestFeatures.Transfer.TRANSFER)
@Features(TestFeatures.TRANSFER)
@Tag(CmdTag.TRANSFER)
@Tag(CampTypeTag.TEXT)
public class TransferBetweenClientsValidationTest {
    @ClassRule
    public static final DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static final String AGENCY_CLIENT = "transfer-ag-cl-1";
    private static final String ANOTHER_AGENCY_CLIENT = "transfer-ag-cl-2";
    private static final String CLIENT_LOGIN = "at-direct-backend-c";
    private static final String ERROR_IS_EXPECTED = "ошибка соответствует ожиданию";
    public final BannersRule bannersRule1 = new TextBannersRule().withUlogin(AGENCY_CLIENT);
    public final BannersRule bannersRule2 = new TextBannersRule().withUlogin(ANOTHER_AGENCY_CLIENT);
    private final BannersRule bannersRule3 = new TextBannersRule().withUlogin(CLIENT_LOGIN);
    @Rule
    public DirectCmdRule cmdRule1 = DirectCmdRule.defaultRule().as(Logins.AGENCY)
            .withRules(bannersRule1, bannersRule2);

    @Rule
    public DirectCmdRule cmdRule2 = DirectCmdRule.defaultRule()
            .withRules(bannersRule3);

    private ClientsCustomOptionsRecord clientsCustomOptionsRecord;

    @Before
    public void before() {

        cmdRule1.getApiStepsRule().as(Logins.SUPER);
        clientsCustomOptionsRecord =
                new ClientsCustomOptionsRecord(Long.parseLong(User.get(Logins.AGENCY_CLIENT).getClientID()),
                        "disallow_money_transfer", 1L);
    }

    @After
    public void after() {
        clientsCustomOptionsRecord.setValue(0L);
        cmdRule1.apiSteps().getDirectJooqDbSteps().useShardForClientId(clientsCustomOptionsRecord.getClientid())
                .clientCustomOptionsSteps().updateClientsCustomOptions(clientsCustomOptionsRecord);
    }

    @Test
    @Description("Проверяем, что нельзя перенести средства с кампаний чужого клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10033")
    public void anotherClientCampaignsFromListTest() {
        cmdRule1.apiSteps().makeCampaignActiveV5(CLIENT_LOGIN, bannersRule3.getCampaignId());
        cmdRule1.apiSteps().campaignFakeSteps().setCampaignSum(
                bannersRule3.getCampaignId(),
                MoneyCurrency.get(User.get(CLIENT_LOGIN).getCurrency()).getMinServicedInvoiceAmount().floatValue());

        cmdRule1.getApiStepsRule().as(Logins.AGENCY);
        cmdRule1.apiSteps().makeCampaignActiveV5(ANOTHER_AGENCY_CLIENT, bannersRule2.getCampaignId());
        cmdRule1.apiSteps().campaignFakeSteps().setCampaignSum(
                bannersRule2.getCampaignId(),
                0F);

        ErrorResponse response = cmdRule1.cmdSteps().transferSteps()
                .getTransferErrorResponse(CLIENT_LOGIN, ANOTHER_AGENCY_CLIENT);
        assertThat(ERROR_IS_EXPECTED, response.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));
    }

    @Test
    @Description("Проверяем, что нельзя перенести средства на кампании чужого клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10034")
    public void anotherClientCampaignsToListTest() {
        cmdRule1.apiSteps().makeCampaignActiveV5(CLIENT_LOGIN, bannersRule3.getCampaignId());
        cmdRule1.apiSteps().campaignFakeSteps().setCampaignSum(
                bannersRule3.getCampaignId(),
                MoneyCurrency.get(User.get(CLIENT_LOGIN).getCurrency()).getMinServicedInvoiceAmount().floatValue());
        cmdRule1.getApiStepsRule().as(Logins.AGENCY);

        cmdRule1.apiSteps().makeCampaignActiveV5(AGENCY_CLIENT, bannersRule1.getCampaignId());
        cmdRule1.apiSteps().campaignFakeSteps().setCampaignSum(
                bannersRule1.getCampaignId(),
                0F);
        ErrorResponse response = cmdRule1.cmdSteps().transferSteps()
                .getTransferErrorResponse(AGENCY_CLIENT, CLIENT_LOGIN);
        assertThat(ERROR_IS_EXPECTED, response.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));
    }

    @Test
    @Description("Проверяем валидацию при client_from, для которого запрещен перенос средств")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10035")
    public void disallowTransferCampaignsFromListTest() {
        updateDisalowTransferAtDb();
        cmdRule1.getApiStepsRule().as(Logins.AGENCY);

        cmdRule1.apiSteps().makeCampaignActiveV5(AGENCY_CLIENT, bannersRule1.getCampaignId());
        cmdRule1.apiSteps().campaignFakeSteps().setCampaignSum(
                bannersRule1.getCampaignId(),
                MoneyCurrency.get(User.get(AGENCY_CLIENT).getCurrency()).getMinServicedInvoiceAmount().floatValue());

        cmdRule1.apiSteps().makeCampaignActiveV5(ANOTHER_AGENCY_CLIENT, bannersRule2.getCampaignId());
        cmdRule1.apiSteps().campaignFakeSteps().setCampaignSum(
                bannersRule2.getCampaignId(),
                0F);
        ErrorResponse response = cmdRule1.cmdSteps().transferSteps()
                .getTransferErrorResponse(Logins.AGENCY_CLIENT, ANOTHER_AGENCY_CLIENT);
        assertThat(ERROR_IS_EXPECTED, response.getError(),
                equalTo(TransferErrors.MONEY_TRANSFER_DISALLOW_FOR_THIS_CLIENT.toString()));
    }

    private void updateDisalowTransferAtDb() {
        try {
            cmdRule1.apiSteps().getDirectJooqDbSteps().useShardForClientId(clientsCustomOptionsRecord.getClientid())
                    .clientCustomOptionsSteps().saveClientsCustomOptions(clientsCustomOptionsRecord);
        } catch (Exception e) {
            cmdRule1.apiSteps().getDirectJooqDbSteps().useShardForClientId(clientsCustomOptionsRecord.getClientid())
                    .clientCustomOptionsSteps().updateClientsCustomOptions(clientsCustomOptionsRecord);
        }
    }
}
