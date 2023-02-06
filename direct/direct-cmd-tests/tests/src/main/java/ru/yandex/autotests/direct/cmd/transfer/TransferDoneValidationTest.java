package ru.yandex.autotests.direct.cmd.transfer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignFromSum;
import ru.yandex.autotests.direct.cmd.data.transfer.CampaignToSum;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferDoneErrors;
import ru.yandex.autotests.direct.cmd.data.transfer.TransferTypeEnum;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.money.MoneyFormat;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.direct.httpclient.util.CommonUtils.sleep;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка ошибок при переносе средств одного клиента (контроллер transfer_done)")
@Stories(TestFeatures.Transfer.TRANSFER_DONE)
@Features(TestFeatures.TRANSFER)
@Tag(CmdTag.TRANSFER_DONE)
@Tag(CampTypeTag.TEXT)
public class TransferDoneValidationTest {

    private static final String CLIENT_LOGIN = "at-direct-transfer-rub";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public TextBannersRule bannersRule1 = new TextBannersRule().withUlogin(CLIENT_LOGIN);
    public TextBannersRule bannersRule2 = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT_LOGIN).withRules(bannersRule1, bannersRule2);

    private Float sumToTransfer;
    private Currency userCurrency;
    private MoneyCurrency moneyCurrency;


    @Before
    public void before() {
        cmdRule.getApiStepsRule().as(CLIENT_LOGIN);
        userCurrency = User.get(CLIENT_LOGIN).getCurrency();
        moneyCurrency = MoneyCurrency.get(userCurrency);
        sumToTransfer = moneyCurrency.getMinTransferAmount().floatValue();
        cmdRule.apiSteps().makeCampaignActiveV5(CLIENT_LOGIN, bannersRule1.getCampaignId());
        cmdRule.apiSteps().campaignFakeSteps().setCampaignSum(bannersRule1.getCampaignId().intValue(),
                moneyCurrency.getMinServicedInvoiceAmount().floatValue());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT_LOGIN);
    }

    @Test
    @Description("Проверяем, что список кампаний, с которого переводим деньги, не может быть пустым")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10040")
    public void EmptyCampaignsFromListValidationTest() {
        CampaignToSum campaignTo = new CampaignToSum()
                .withObjectId(String.valueOf(bannersRule1.getCampaignId()))
                .withCampaignToSum(String.valueOf(sumToTransfer));
        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps()
                .transferDone(null, singletonList(campaignTo), TransferTypeEnum.FROM_MANY_TO_ONE);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TransferDoneErrors.TRANSFER_DISALLOW_CAMPAIGNS_DOESNT_SELECTED.toString()));
    }

    @Test
    @Description("Проверяем, что список кампаний, на который переводим деньги, не может быть пустым")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10041")
    public void EmptyCampaignsToListValidationTest() {
        CampaignFromSum campaignFrom = new CampaignFromSum()
                .withObjectId(String.valueOf(bannersRule1.getCampaignId()))
                .withCampaignFromSum(String.valueOf(sumToTransfer));
        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps()
                .transferDone(singletonList(campaignFrom), null, TransferTypeEnum.FROM_ONE_TO_MANY);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TransferDoneErrors.TRANSFER_DISALLOW_CAMPAIGNS_DOESNT_SELECTED.toString()));
    }


    @Test
    @Description("Проверяем, что должна быть указана сумма переноса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10042")
    public void emptySumValidationTest() {
        CampaignFromSum campaignFrom = new CampaignFromSum()
                .withObjectId(String.valueOf(bannersRule1.getCampaignId()))
                .withCampaignFromSum("");
        CampaignToSum campaignTo = new CampaignToSum()
                .withObjectId(String.valueOf(bannersRule2.getCampaignId()));

        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps()
                .transferDone(singletonList(campaignFrom), singletonList(campaignTo),
                        TransferTypeEnum.FROM_MANY_TO_ONE);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TransferDoneErrors.TRANSFER_DISALLOW_CAMPAIGNS_DOESNT_SELECTED.toString()));
    }

    @Test
    @Description("Проверяем, что при переносе с нескольких кампаний на одну должна быть указана кампания, на которую переносят деньги")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10043")
    public void emptyCampaignIdValidationTest() {
        CampaignFromSum campaignFrom = new CampaignFromSum()
                .withCampaignFromSum(String.valueOf(sumToTransfer));

        CampaignToSum campaignTo = new CampaignToSum()
                .withObjectId(String.valueOf(bannersRule2.getCampaignId()));

        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps().transferDone(singletonList(campaignFrom),
                singletonList(campaignTo), TransferTypeEnum.FROM_MANY_TO_ONE);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TransferDoneErrors.TRANSFER_DISALLOW_CAMPAIGNS_DOESNT_SELECTED.toString()));
    }

    @Test
    @Description("Проверяем, что сумма переноса не может быть меньше минимальной")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10044")
    public void minSumValidationTest() {
        waitAndCheckCampaignSum(bannersRule1.getCampaignId().intValue(), not(equalTo(0L)));

        int invoiceId = cmdRule.apiSteps().createInvoice(bannersRule1.getCampaignId().intValue(),
                moneyCurrency.getMinServicedInvoiceAmount().floatValue(), userCurrency);
        cmdRule.apiSteps().balanceSteps().turnOnInvoice(invoiceId);
        cmdRule.apiSteps().makeCampaignActiveV5(CLIENT_LOGIN, bannersRule2.getCampaignId());

        CampaignFromSum campaignFrom = new CampaignFromSum()
                .withObjectId(String.valueOf(bannersRule1.getCampaignId()))
                .withCampaignFromSum(String.valueOf(moneyCurrency.getMinTransferAmount().getPrevious().floatValue()));

        CampaignToSum campaignTo = new CampaignToSum().withObjectId(String.valueOf(bannersRule2.getCampaignId()));

        RedirectResponse response = cmdRule.cmdSteps().transferDoneSteps().transferDone(singletonList(campaignFrom),
                singletonList(campaignTo), TransferTypeEnum.FROM_MANY_TO_ONE);
        assertThat("Ошибка в ответе контроллера соответсвует ожиданиям", response.getLocationParam(LocationParam.ERROR),
                equalTo(TextResourceFormatter.resource(TransferDoneErrors.TRANSFER_SUM_SHOULD_BE_MORE_THAN).args(
                        moneyCurrency.getMinTransferAmount().
                                stringValue(MoneyFormat.TWO_DIGITS_POINT_SEPARATED_NBSP) + " " +
                                MoneyCurrency.get(userCurrency).getAbbreviation("ru"))
                        .toString().replace("\\n", "\n")));
    }

    private void waitAndCheckCampaignSum(Integer campaignId, Matcher matcher) {
        final int ATTEMPTS = 150;
        final int TIMEOUT = 2_000;
        int currentAttempt = ATTEMPTS;

        while (currentAttempt > 0) {
            Long sum = TestEnvironment.newDbSteps().campaignsSteps().getCampaignById(Long.valueOf(campaignId)).getSum().longValue();
            if (!matcher.matches(sum)) {
                sleep(TIMEOUT);
            } else {
                break;
            }
            currentAttempt--;
        }
        MatcherAssert
                .assertThat("Исчерпано кол-во попыток обновить кампании", currentAttempt, not(lessThanOrEqualTo(0)));
    }

}
