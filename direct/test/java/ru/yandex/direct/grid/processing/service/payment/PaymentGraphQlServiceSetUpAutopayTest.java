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

import ru.yandex.direct.balance.client.model.response.GetCardBindingURLResponse;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.payment.GdSetUpAutopay;
import ru.yandex.direct.grid.processing.model.payment.GdSetUpAutopayPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.defect.NumberDefects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.TURN_ON_AUTOPAY;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PaymentGraphQlServiceSetUpAutopayTest {

    private static final String MUTATION_HANDLE = "setUpAutopay";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    bindingUrl\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String EXPECTED_BINDING_URL = "https://ya.ru/test_binding_url";


    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DbQueueSteps dbQueueSteps;
    @Autowired
    private Steps steps;
    private User operator;
    private BigDecimal minPaySum;


    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        CurrencyCode currency = clientService.getWorkCurrency(clientInfo.getClientId()).getCode();
        minPaySum = Currencies.getCurrency(currency).getMinAutopay();

        TestAuthHelper.setDirectAuthentication(operator);
        steps.campaignSteps().createWalletCampaign(clientInfo);


        when(balanceService.getCardBinding(eq(operator.getUid()), any(), any(), anyBoolean()))
                .thenReturn(new GetCardBindingURLResponse(EXPECTED_BINDING_URL, null));

        dbQueueSteps.registerJobType(TURN_ON_AUTOPAY);
        dbQueueSteps.clearQueue(TURN_ON_AUTOPAY);


    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void setUpAutopay_Success() {
        GdSetUpAutopay input = new GdSetUpAutopay()
                .withPaymentSum(minPaySum.add(BigDecimal.ONE))
                .withRemainingSum(BigDecimal.ONE)
                .withIsMobile(false)
                .withIsLegalPerson(false);
        GdSetUpAutopayPayload payload = setUpAutopayGraphQl(input);
        assertThat(payload.getBindingUrl()).isEqualTo(EXPECTED_BINDING_URL);
        assertThat(payload.getValidationResult().getErrors().size()).isZero();
    }

    @Test
    public void setUpAutopay_PaySumIsZero_ValidationErrors() {
        GdSetUpAutopay input = new GdSetUpAutopay()
                .withPaymentSum(BigDecimal.ZERO)
                .withRemainingSum(BigDecimal.ONE)
                .withIsMobile(false)
                .withIsLegalPerson(false);
        GdSetUpAutopayPayload payload = setUpAutopayGraphQl(input);
        assertThat(payload.getBindingUrl()).isNull();

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdSetUpAutopay.PAYMENT_SUM)),
                NumberDefects.greaterThanOrEqualTo(minPaySum),
                true);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    private GdSetUpAutopayPayload setUpAutopayGraphQl(GdSetUpAutopay request) {
        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);

        return GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE), GdSetUpAutopayPayload.class);
    }

}
