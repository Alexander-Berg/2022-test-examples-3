package ru.yandex.direct.grid.processing.service.payment;

import java.math.BigDecimal;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.service.integration.balance.model.CreateAndPayRequestResult;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.payment.GdGetPaymentFormUrl;
import ru.yandex.direct.grid.processing.model.payment.GdGetPaymentFormUrlPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PaymentGraphQlServiceGetPaymentFormUrlTest {
    private static final String QUERY_HANDLE = "getPaymentFormUrl";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n" +
            "  %s (input: %s) {\n" +
            "    paymentUrl\n" +
            "  }\n" +
            "}\n";
    private static final String EXPECTED_PAYMENT_URL = "https://ya.ru/test_payment_url";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private Steps steps;
    private User operator;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        steps.campaignSteps().createWalletCampaign(clientInfo);
        Long personId = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        when(balanceService.getOrCreatePerson(any(), any(), anyBoolean()))
                .thenReturn(personId);

        when(balanceService.createAndPayRequest(any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyBoolean()))
                .thenAnswer(invocation ->
                        new CreateAndPayRequestResult(EXPECTED_PAYMENT_URL, invocation.getArgument(4), null));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getPaymentFormUrl_Success() {
        GdGetPaymentFormUrl input = new GdGetPaymentFormUrl()
                .withIsMobile(false)
                .withPaymentSum(BigDecimal.TEN)
                .withIsLegalPerson(false);

        GdGetPaymentFormUrlPayload payload = getPaymentFormUrlGraphQl(input);
        assertThat(payload.getPaymentUrl()).isEqualTo(EXPECTED_PAYMENT_URL);
    }

    private GdGetPaymentFormUrlPayload getPaymentFormUrlGraphQl(GdGetPaymentFormUrl input) {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE, graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);
        return GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE), GdGetPaymentFormUrlPayload.class);
    }
}
