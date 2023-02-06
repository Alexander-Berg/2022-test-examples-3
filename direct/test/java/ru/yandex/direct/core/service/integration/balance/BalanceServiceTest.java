package ru.yandex.direct.core.service.integration.balance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.BalanceXmlRpcClient;
import ru.yandex.direct.balance.client.exception.BalanceClientException;
import ru.yandex.direct.balance.client.model.method.BaseBalanceMethodSpec;
import ru.yandex.direct.balance.client.model.request.BalanceRpcRequestParam;
import ru.yandex.direct.balance.client.model.request.CreateOrUpdateOrdersBatchRequest;
import ru.yandex.direct.balance.client.model.request.ListPaymentMethodsSimpleRequest;
import ru.yandex.direct.balance.client.model.response.ListPaymentMethodsSimpleResponseItem;
import ru.yandex.direct.core.entity.payment.model.CardInfo;
import ru.yandex.direct.core.service.integration.balance.model.PaymentMethodType;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.service.integration.balance.model.PaymentMethodType.CARD;
import static ru.yandex.direct.core.service.integration.balance.model.PaymentMethodType.OVERDRAFT;
import static ru.yandex.direct.core.service.integration.balance.model.PaymentMethodType.UNKNOWN;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.notEquals;

public class BalanceServiceTest {

    private static final int FAKE_SERVICE_ID = 667;
    private static final String FAKE_SERVICE_TOKEN = "fake_token";
    private static final Long FAKE_UID = 123L;
    private static final String USER_IP = "127.0.0.1";
    private BalanceXmlRpcClient balanceXmlRpcClient;
    private BalanceXmlRpcClient balanceSimpleXmlRpcClient;
    private BalanceService balanceService;

    @Before
    public void setUp() throws Exception {
        balanceXmlRpcClient = mock(BalanceXmlRpcClient.class);
        balanceSimpleXmlRpcClient = mock(BalanceXmlRpcClient.class);
        BalanceClient balanceClient = new BalanceClient(balanceXmlRpcClient, balanceSimpleXmlRpcClient);
        balanceService = new BalanceService(balanceClient, FAKE_SERVICE_ID, FAKE_SERVICE_TOKEN);
    }

    @Test
    public void getUserPaymentMethodTypes_checkReturnInfo_success() {
        PaymentMethodType[] expectedPaymentMethodTypeTypes = new PaymentMethodType[]{CARD, OVERDRAFT, UNKNOWN};

        Map<String, Map<String, Object>> paymentMethods = StreamEx.of(expectedPaymentMethodTypeTypes)
                .map(method -> notEquals(method, UNKNOWN) ? method.getBalanceName() : "some_unknown_method")
                .map(type -> Map.of("type", (Object) type))
                .zipWith(IntStreamEx.range(1, Integer.MAX_VALUE)
                        .boxed()
                        .map(n -> "PaymentMethod#" + n))
                .invert()
                .toMap();
        //noinspection unchecked
        when(balanceXmlRpcClient.call(any(BaseBalanceMethodSpec.class), any(BalanceRpcRequestParam.class)))
                .thenReturn(new ListPaymentMethodsSimpleResponseItem()
                        .withPaymentMethods(paymentMethods));

        Set<PaymentMethodType> returnedPaymentMethodTypeTypes = balanceService.getUserPaymentMethodTypes(FAKE_UID);
        assertThat(returnedPaymentMethodTypeTypes).containsExactlyInAnyOrder(expectedPaymentMethodTypeTypes);
    }

    @Test
    public void getUserPaymentMethodTypes_checkSendInfo_success() {
        ListPaymentMethodsSimpleRequest expectedRequest = new ListPaymentMethodsSimpleRequest()
                .withServiceToken(FAKE_SERVICE_TOKEN)
                .withUid(FAKE_UID)
                .withUserIp(USER_IP);
        String expectedHandleName = "Balance2.ListPaymentMethodsSimple";
        ArrayList<Object> argumentsList = new ArrayList<>();

        //noinspection unchecked
        when(balanceXmlRpcClient.call(any(BaseBalanceMethodSpec.class), any(BalanceRpcRequestParam.class)))
                .then(invocation -> {
                    argumentsList.addAll(Arrays.asList(invocation.getArguments()));
                    return new ListPaymentMethodsSimpleResponseItem()
                            .withPaymentMethods(emptyMap());
                });
        balanceService.getUserPaymentMethodTypes(FAKE_UID);

        BaseBalanceMethodSpec methodSpec = (BaseBalanceMethodSpec) argumentsList.get(0);
        String sentMethodName = methodSpec.getFullName();
        Object sentRequest = argumentsList.get(1);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(sentMethodName)
                    .as("sent method name")
                    .isEqualTo(expectedHandleName);
            soft.assertThat(sentRequest).as("sent request")
                    .is(matchedBy(beanDiffer(expectedRequest).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void createOrUpdateOrders_onException_ReturnFalse() {
        doThrow(new BalanceClientException("")).when(balanceXmlRpcClient).call(any(), any());

        boolean result = balanceService.createOrUpdateOrders(new CreateOrUpdateOrdersBatchRequest());

        assertThat(result).isFalse();
    }

    @Test
    public void createOrUpdateOrders_allResponseItemsHasNoErrors_ReturnTrue() {
        String[] success = new String[]{"0", "Success"};
        String[][] response = new String[][]{success};
        //noinspection unchecked
        doReturn(response).when(balanceXmlRpcClient).call(any(BaseBalanceMethodSpec.class),
                any(BalanceRpcRequestParam.class));

        boolean result = balanceService.createOrUpdateOrders(new CreateOrUpdateOrdersBatchRequest());

        assertThat(result).isTrue();
    }

    @Test
    public void createOrUpdateOrders_OneResponseItemsHasError_ReturnFalse() {
        String[] success = new String[]{"0", "Success"};
        String[] error = new String[]{"-1", "Error"};
        String[][] response = new String[][]{success, error};

        //noinspection unchecked
        doReturn(response).when(balanceXmlRpcClient).call(any(BaseBalanceMethodSpec.class),
                any(BalanceRpcRequestParam.class));

        boolean result = balanceService.createOrUpdateOrders(new CreateOrUpdateOrdersBatchRequest());

        assertThat(result).isFalse();
    }

    @Test
    public void getUserCards() {
        //noinspection unchecked
        when(balanceSimpleXmlRpcClient.call(any(BaseBalanceMethodSpec.class), any(BalanceRpcRequestParam.class)))
                .thenReturn(
                        new ListPaymentMethodsSimpleResponseItem()
                                .withPaymentMethods(Map.of("card-123", Map.of(
                                        "type", "card",
                                        "card_id", "card-123",
                                        "system", "visa",
                                        "currency", "rub",
                                        "number", "123****456"
                                ))));
        List<CardInfo> resultCards = balanceService.getUserCards(123L);

        assertThat(resultCards).containsExactly(
                new CardInfo()
                        .withCardId("card-123")
                        .withSystem("visa")
                        .withCurrency("rub")
                        .withMaskedNumber("123****456")
        );
    }
}
