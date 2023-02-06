package ru.yandex.direct.grid.processing.service.payment;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.jooq.DSLContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.payment.GdRemoveAutopayPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.dbschema.ppc.Tables.AUTOPAY_SETTINGS;
import static ru.yandex.direct.dbschema.ppc.Tables.WALLET_CAMPAIGNS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PaymentGraphQlServiceRemoveAutopaymentTest {
    private static final String QUERY_HANDLE = "removeAutopay";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n" +
            "  removeAutopay {\n" +
            "    validationResult {\n" +
            "      errors{code}\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;

    private User operator;
    private DSLContext dslContext;
    private CampaignInfo walletInfo;
    private Long walletCid;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        walletInfo = steps.campaignSteps()
                .createCampaign(new CampaignInfo()
                        .withCampaign(activeWalletCampaign(operator.getClientId(), operator.getUid()))
                        .withClientInfo(clientInfo)
                );
        walletCid = walletInfo.getCampaignId();
        steps.campaignSteps().addFakeAutoPay(walletInfo, operator, true);

        dslContext = dslContextProvider.ppc(clientInfo.getShard());

        assertThat(getAutopayMode(walletCid))
                .isEqualTo(WalletCampaignsAutopayMode.min_balance);

        assertThat(isSettingsAvailable(walletCid))
                .isTrue();
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void removeAutopay_OneCall() {
        GdRemoveAutopayPayload payload = processQuery();
        checkAfterRemoval(payload);
    }

    @Test
    public void removeAutopay_TwoCalls() {
        processQuery();
        GdRemoveAutopayPayload payload = processQuery();
        checkAfterRemoval(payload);
    }

    private void checkAfterRemoval(GdRemoveAutopayPayload payload) {
        assertThat(payload.getValidationResult())
                .isNull();

        assertThat(getAutopayMode(walletCid))
                .isEqualTo(WalletCampaignsAutopayMode.none);

        assertThat(isSettingsAvailable(walletCid))
                .isFalse();
    }

    private boolean isSettingsAvailable(Long walletCid) {
        return dslContext.select(AUTOPAY_SETTINGS.WALLET_CID)
                .from(AUTOPAY_SETTINGS)
                .where(AUTOPAY_SETTINGS.WALLET_CID.eq(walletCid))
                .fetchAny() != null;
    }

    private WalletCampaignsAutopayMode getAutopayMode(Long walletCid) {
        return dslContext.select(WALLET_CAMPAIGNS.AUTOPAY_MODE)
                .from(WALLET_CAMPAIGNS)
                .where(WALLET_CAMPAIGNS.WALLET_CID.eq(walletCid))
                .fetchOne(WALLET_CAMPAIGNS.AUTOPAY_MODE);
    }

    private GdRemoveAutopayPayload processQuery() {
        ExecutionResult result = processor.processQuery(null, QUERY_TEMPLATE, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);
        return GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE), GdRemoveAutopayPayload.class);
    }
}
