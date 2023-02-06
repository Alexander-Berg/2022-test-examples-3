package ru.yandex.direct.grid.processing.service.payment;

import java.util.List;
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

import ru.yandex.direct.core.entity.payment.model.CardInfo;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.payment.GdClientPaymentCard;
import ru.yandex.direct.grid.processing.model.payment.GdGetClientPaymentCardsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PaymentGraphQlServiceGetClientPaymentCardsTest {
    private static final String QUERY_HANDLE = "getClientPaymentCards";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n" +
            "  %s {\n" +
            "    cards{\n" +
            "      cardId\n" +
            "      maskedPan\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private Steps steps;
    private User operator;
    private GdClientPaymentCard expectedGdClientPaymentCard;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        CardInfo cardInfo = new CardInfo()
                .withCardId("card-1")
                .withCurrency("RUB")
                .withMaskedNumber("1xx");

        expectedGdClientPaymentCard = new GdClientPaymentCard()
                .withCardId(cardInfo.getCardId())
                .withMaskedPan(cardInfo.getMaskedNumber());

        when(balanceService.getClientPaymentCards(eq(operator.getUid()), any()))
                .thenReturn(List.of(cardInfo));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getClientCards_OneCard() {
        GdGetClientPaymentCardsPayload payload = getPayCardsGraphQl();
        assertThat(payload.getCards().size()).isEqualTo(1);
        assertThat(payload.getCards().get(0)).isEqualTo(expectedGdClientPaymentCard);
    }

    private GdGetClientPaymentCardsPayload getPayCardsGraphQl() {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE);
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);
        return GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE), GdGetClientPaymentCardsPayload.class);
    }
}
