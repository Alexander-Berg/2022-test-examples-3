package ru.yandex.direct.grid.processing.service.payment;

import java.math.BigDecimal;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.payment.model.AutopayParams;
import ru.yandex.direct.core.entity.payment.model.AutopaySettingsPaymethodType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestWalletCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.payment.GdAutopayStatus;
import ru.yandex.direct.grid.processing.model.payment.GdGetAutopaySettingsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PaymentGraphQlServiceGetAutopaySettingsTest {
    private static final String QUERY_HANDLE = "getAutopaySettings";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n" +
            "  %s {\n" +
            "    cardId\n" +
            "    paymentSum\n" +
            "    remainingSum\n" +
            "    autopayStatus\n" +
            "  }\n" +
            "}\n";
    private AutopayParams defaultAutopayParams;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private TestWalletCampaignRepository testWalletCampaignRepository;
    @Autowired
    private Steps steps;
    private User operator;
    private Long walletCid;
    private int shard;
    private long uid;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        uid = clientInfo.getUid();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        walletCid = steps.campaignSteps().createWalletCampaign(clientInfo).getCampaignId();

        defaultAutopayParams = new AutopayParams()
                .withCardId("x-123")
                .withPaymentSum(BigDecimal.TEN)
                .withPaymentType(AutopaySettingsPaymethodType.CARD)
                .withPersonId(123L)
                .withRemainingSum(BigDecimal.ONE);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getAutopaySettings_AutopayOff() {
        GdGetAutopaySettingsPayload payload = getAutopaySettingsGraphQl();
        assertThat(payload.getAutopayStatus()).isEqualTo(GdAutopayStatus.OFF);
        assertThat(payload.getCardId()).isNull();
        assertThat(payload.getPaymentSum()).isNull();
        assertThat(payload.getRemainingSum()).isNull();
    }

    @Test
    public void getAutopaySettings_AutopayOn() {
        testWalletCampaignRepository.addDefaultWallet(shard, walletCid);
        walletRepository.turnOnAutopay(shard, walletCid, uid, defaultAutopayParams);
        GdGetAutopaySettingsPayload payload = getAutopaySettingsGraphQl();

        assertThat(payload.getAutopayStatus()).isEqualTo(GdAutopayStatus.ON);
        assertThat(payload.getCardId()).isEqualTo(defaultAutopayParams.getCardId());
        assertThat(payload.getPaymentSum()).isCloseTo(defaultAutopayParams.getPaymentSum(), withinPercentage(1));
        assertThat(payload.getRemainingSum()).isCloseTo(defaultAutopayParams.getRemainingSum(), withinPercentage(1));
    }

    private GdGetAutopaySettingsPayload getAutopaySettingsGraphQl() {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE);
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);
        return GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE), GdGetAutopaySettingsPayload.class);
    }
}
