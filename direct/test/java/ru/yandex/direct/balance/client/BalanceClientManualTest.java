package ru.yandex.direct.balance.client;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.balance.client.exception.BalanceClientException;
import ru.yandex.direct.balance.client.model.request.AgencySelectPolicy;
import ru.yandex.direct.balance.client.model.request.FindClientRequest;
import ru.yandex.direct.balance.client.model.request.ListPaymentMethodsSimpleRequest;
import ru.yandex.direct.balance.client.model.response.BalanceBankDescription;
import ru.yandex.direct.balance.client.model.response.ClientPassportInfo;
import ru.yandex.direct.balance.client.model.response.FindClientResponseItem;
import ru.yandex.direct.balance.client.model.response.ListPaymentMethodsSimpleResponseItem;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Disabled("Ходит во внешний сервис, должен запускаться только локально вручную")
class BalanceClientManualTest {
    private BalanceClient balanceClient;

    private static void printResponse(Object object) {
        System.out.println(toString(object));
    }

    private static String toString(Object object) {
        return ToStringBuilder.reflectionToString(object, ToStringStyle.MULTI_LINE_STYLE, true);
    }

    @BeforeEach
    void setUp() throws Exception {
        String urlString = "http://greed-tm.paysys.yandex.ru:8002/xmlrpc";
        URL serverUrl = new URL(urlString);
        BalanceXmlRpcClientConfig config = new BalanceXmlRpcClientConfig(serverUrl)
                .withConnectionTimeout(ofSeconds(4))
                .withMaxRetries(1)
                .withRequestTimeout(ofSeconds(300));
        BalanceXmlRpcClient balanceXmlRpcClient = new BalanceXmlRpcClient(config);
        balanceClient = new BalanceClient(balanceXmlRpcClient, null);
    }

    @Test
    void getBank_success() {
        BalanceBankDescription bank = balanceClient.getBank("RZBRROBUXXX"); // Райффайзен банк
        printResponse(bank);
        assertThat(bank).isNotNull();
    }

    /**
     * Тест вызова ручки Balance2.Balance2.FindClient
     */
    @Test
    void findClient_success() {
        long uid = 123;
        FindClientRequest request = new FindClientRequest()
                .withUid(uid)
                .withAgencySelectPolicy(AgencySelectPolicy.ASP_ALL)
                .withPrimaryClients(true);
        List<FindClientResponseItem> client = balanceClient.findClient(request);
        printResponse(client);
        assertThat(client).isNotNull();
    }

    /**
     * Тест вызова ручки Balance2.FindClient
     */
    @Test
    void findClient_whenUidIsMoreThenInt32_success() {
        long uid = 1130000041180367L;
        FindClientRequest request = new FindClientRequest()
                .withUid(uid)
                .withAgencySelectPolicy(AgencySelectPolicy.ASP_ALL)
                .withPrimaryClients(true);
        List<FindClientResponseItem> client = balanceClient.findClient(request);
        printResponse(client);
        assertThat(client).isNotNull();
    }

    /**
     * Тест ручки Balance2.ListPaymentMethodsSimple.
     */
    @Test
    void listPaymentMethodsSimple_success() {
        ListPaymentMethodsSimpleRequest request = new ListPaymentMethodsSimpleRequest()
                .withServiceToken("PPC_d5bc1fbe74a173f4688d24fc6bf8399a")
                .withUid(123L)
                .withUserIp("127.0.0.1");
        ListPaymentMethodsSimpleResponseItem response = balanceClient.listPaymentMethodsSimple(request);
        printResponse(response);
        assertThat(response).isNotNull();
    }

    @Test
    void listPaymentMethodsSimple_whenUidIsMoreThenInt32_success() {
        ListPaymentMethodsSimpleRequest request = new ListPaymentMethodsSimpleRequest()
                .withServiceToken("PPC_d5bc1fbe74a173f4688d24fc6bf8399a")
                .withUid(1130000041180367L)
                .withUserIp("127.0.0.1");
        ListPaymentMethodsSimpleResponseItem response = balanceClient.listPaymentMethodsSimple(request);
        printResponse(response);
        assertThat(response).isNotNull();
    }

    /**
     * Тест вызова ручки Balance2.CreateUserClientAssociation
     * Вернёт исключение на несуществующих пользователе или клиенте, поэтому в тесте использованы существующие
     * пользователь и клиент.
     */
    @Test
    void createUserClientAssociation_success() {
        long operatorUid = 123L;
        long clientId = 7350895L;
        long representativeUid = 123L;

        /* В случае исключения выводим весь stack trace, со вложенными исключениями, т.к. в них суть ошибки, а
        assertThatCode выводит только верхнее исключение.*/
        ThrowableAssert.ThrowingCallable callable =
                () -> balanceClient.createUserClientAssociation(operatorUid, clientId, representativeUid);

        assertThatCode(callable).as("invoke BalanceClient#createUserClientAssociation")
                .doesNotThrowAnyException();
    }

    /**
     * Тест вызова ручки Balance2.CreateUserClientAssociation
     * Вернёт исключение на несуществующих пользователе или клиенте, поэтому в тесте использованы существующие
     * пользователь и клиент.
     * Т.к. ручки баланса идемпотентны, то отсутствие связи между пользователем и клиентом, перед вызововом метода,
     * не требуется.
     */
    @Test
    void createUserClientAssociation_whenUidIsMoreThenInt32_success() {
        long operatorUid = 1130000041180367L;
        long clientId = 109881497L;
        long representativeUid = 1130000041180367L;

        /* В случае исключения выводим весь stack trace, со вложенными исключениями, т.к. в них суть ошибки, а
        assertThatCode выводит только верхнее исключение.*/
        ThrowableAssert.ThrowingCallable callable =
                () -> balanceClient.createUserClientAssociation(operatorUid, clientId, representativeUid);

        assertThatCode(callable).as("invoke BalanceClient#createUserClientAssociation")
                .doesNotThrowAnyException();
    }

    /**
     * Тест вызова ручки Balance2.RemoveUserClientAssociation
     * Вернёт исключение на несуществующих пользователе или клиенте, поэтому в тесте использованы существующие
     * пользователь и клиент.
     * Т.к. ручки баланса идемпотентны, то существование связи между пользователем и клиентом, перед вызововом метода,
     * не требуется.
     */
    @Test
    void removeUserClientAssociation_success() {
        long operatorUid = 123L;
        long clientId = 7350895L;
        long representativeUid = 123L;

        /* В случае исключения выводим весь stack trace, со вложенными исключениями, т.к. в них суть ошибки, а
        assertThatCode выводит только верхнее исключение.*/
        ThrowableAssert.ThrowingCallable callable =
                () -> balanceClient.removeUserClientAssociation(operatorUid, clientId, representativeUid);

        // Восстанавливаем связь.
        try {
            balanceClient.createUserClientAssociation(operatorUid, clientId, representativeUid);
        } catch (Exception ignored) {
        }

        assertThatCode(callable).as("invoke BalanceClient#createUserClientAssociation")
                .doesNotThrowAnyException();
    }

    /**
     * Тест вызова ручки Balance2.GetPassportByUid
     */
    @Test
    void getPassportByUid_success() {
        long operatorUid = 123L;
        long uid = 123L;
        ClientPassportInfo response = balanceClient.getPassportByUid(operatorUid, uid);
        printResponse(response);
        assertThat(response)
                .as("GetPassportByUid response")
                .isNotNull();
    }

    /**
     * Тест вызова ручки Balance2.GetPassportByUid
     */
    @Test
    void getPassportByUid_whenUidIsMoreThenInt32_success() {
        long operatorUid = 1130000041180367L;
        long uid = 1130000041180367L;
        ClientPassportInfo response = balanceClient.getPassportByUid(operatorUid, uid);
        printResponse(response);
        assertThat(response)
                .as("GetPassportByUid response")
                .isNotNull();
    }

    /**
     * Тест вызова ручки Balance2.GetPassportByUid с несуществующим uid'ом
     */
    @Test
    void getPassportByUid_whenUidIsNotExist_failure() {
        long notExistingUid = Integer.MAX_VALUE - 1;
        assertThatCode(() -> balanceClient.getPassportByUid(notExistingUid, notExistingUid))
                .isInstanceOf(BalanceClientException.class);
    }

    /**
     * Тест вызова ручки Balance2.ListClientPassports
     */
    @Test
    void getClientRepresentativePassports_success() {
        long operatorUid = 123L;
        long clientId = 7350895;
        List<ClientPassportInfo> response = balanceClient.getClientRepresentativePassports(operatorUid, clientId);
        printResponse(response);
        assertThat(response)
                .as("GetPassportByUid response")
                .isNotNull();
    }

    /**
     * Тест вызова ручки Balance2.EditPassport
     */
    @Test
    void editPassport_whenChangeIsMainProperty_success() {
        long uid = 123L;
        //Проверяем исходное состояние
        ClientPassportInfo srcInfo = balanceClient.getPassportByUid(uid, uid);
        checkNotNull(srcInfo);
        checkState(srcInfo.getIsMain() == 0, "Bad start state, isMain must be zero");
        ClientPassportInfo expectedInfo = copyClientPassportInfo(srcInfo).withIsMain(1);

        //Выполняем запрос и читаем новое состояние
        ClientPassportInfo newInfo = new ClientPassportInfo().withIsMain(1);
        balanceClient.editPassport(uid, uid, newInfo);
        ClientPassportInfo actualInfo = balanceClient.getPassportByUid(uid, uid);

        //Сразу возвращаем в исходное состояние
        balanceClient.editPassport(uid, uid, new ClientPassportInfo().withIsMain(0));

        //Проверяем, какое состояние было после запроса
        assertThat(actualInfo).as("actual ClientPassportInfo")
                .is(matchedBy(beanDiffer(expectedInfo).useCompareStrategy(onlyExpectedFields())));
    }

    private ClientPassportInfo copyClientPassportInfo(ClientPassportInfo srcInfo) {
        return new ClientPassportInfo()
                .withIsMain(srcInfo.getIsMain())
                .withLogin(srcInfo.getLogin())
                .withName(srcInfo.getName())
                .withUid(srcInfo.getUid())
                .withClientId(srcInfo.getClientId());
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @Test
    void getManagersInfo_success() {
        assertThatCode(() -> {
            Set response = balanceClient.massCheckManagersExist(List.of(7610043L, 9256931L, 9704335L));
            printResponse(response);
        }).doesNotThrowAnyException();
    }
}
