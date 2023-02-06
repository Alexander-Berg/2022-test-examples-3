package ru.yandex.direct.intapi.entity.payment.controller;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.promocodes.service.PromocodeHelper;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.service.integration.balance.model.CreateAndPayRequestResult;
import ru.yandex.direct.core.service.integration.balance.model.CreateInvoiceRequestResult;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.client.IntApiClient;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.payment.service.IntapiPaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PaymentControllerTest {
    private static final String EXPECTED_PAYMENT_URL = "payment_url";
    private static final String EXPECTED_MOBILE_PAYMENT_URL = EXPECTED_PAYMENT_URL + "?template_tag=mobile%2Fform";
    private static final Long EXPECTED_INVOICE_ID = 123456789L;
    private static final Long EXPECTED_PAYMENT_SUM = 1234567L;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ClientService clientService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private PromocodeHelper promocodeHelper;
    private MockMvc mockMvc;
    private Long userId;
    private String userLogin;
    private boolean isLegalPerson;
    private boolean isInvoicePayment;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        userId = campaignInfo.getUid();
        userLogin = campaignInfo.getClientInfo().getChiefUserInfo().getLogin();
        isLegalPerson = false;
        isInvoicePayment = false;

        balanceService = spy(balanceService);
        doReturn(new CreateAndPayRequestResult(EXPECTED_PAYMENT_URL, null, null))
                .when(balanceService)
                .createAndPayRequest(
                        eq(userId), eq(campaignInfo.getClientInfo().getClientId()), eq(campaignInfo.getCampaignId()),
                        any(), any(), any(), any(), anyBoolean(), nullable(String.class), anyBoolean());
        doReturn(new CreateInvoiceRequestResult(EXPECTED_INVOICE_ID, null))
                .when(balanceService)
                .createInvoice(
                        eq(userId), eq(campaignInfo.getClientInfo().getClientId()), eq(campaignInfo.getCampaignId()),
                        any(), any(), any(), anyBoolean(), nullable(String.class), anyBoolean());
        IntApiClient intApiClient = mock(IntApiClient.class);
        when(intApiClient.validatePayCamp(any(), any(), any(), any())).thenReturn(true);
        IntapiPaymentService paymentService = new IntapiPaymentService(userService,
                shardHelper, clientService, walletRepository,
                balanceService, intApiClient, promocodeHelper);
        PaymentController paymentController = new PaymentController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    public void checkPaymentFormUrlWithUid_Success() throws Exception {
        var request = String.format("{\"uid\": %d, \"legal_entity\": %b, \"is_invoice_payment\": %b, " +
                "\"payment_sum\": %d}", userId, isLegalPerson, isInvoicePayment, EXPECTED_PAYMENT_SUM);
        checkPaymentFormUrlOrInvoiceId(request, EXPECTED_PAYMENT_URL, EXPECTED_MOBILE_PAYMENT_URL, null);
    }

    @Test
    public void checkPaymentFormUrlWithLogin_Success() throws Exception {
        var request = String.format("{\"login\": \"%s\", \"legal_entity\": %b, \"is_invoice_payment\": %b, " +
                "\"payment_sum\": %d}", userLogin, isLegalPerson, isInvoicePayment, EXPECTED_PAYMENT_SUM);
        checkPaymentFormUrlOrInvoiceId(request, EXPECTED_PAYMENT_URL, EXPECTED_MOBILE_PAYMENT_URL, null);
    }

    @Test
    public void checkInvoiceIdWithLogin_Success() throws Exception {
        isInvoicePayment = true;
        var paysysId = 1001;
        var request = String.format("{\"login\": \"%s\", \"legal_entity\": %b, \"is_invoice_payment\": %b," +
                        "\"paysys_id\": %d, \"payment_sum\": %d}", userLogin, isLegalPerson, isInvoicePayment, paysysId,
                EXPECTED_PAYMENT_SUM);
        checkPaymentFormUrlOrInvoiceId(request, null, null, EXPECTED_INVOICE_ID);
    }

    private void checkPaymentFormUrlOrInvoiceId(String input, String paymentUrl, String mobilePaymentUrl,
                                                Long invoiceId) throws Exception {
        JSONObject content = new JSONObject();
        content.put("payment_url", paymentUrl);
        content.put("mobile_payment_url", mobilePaymentUrl);
        content.put("invoice_id", invoiceId);

        mockMvc
                .perform(post("/payment/getPaymentFormUrlOrInvoiceId")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(input))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(content.toString()));
    }
}
