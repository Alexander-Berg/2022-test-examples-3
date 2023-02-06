package ru.yandex.market.autopayment;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.autopayment.AutoPaymentService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.common.balance.model.BalanceException;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.balance.xmlrpc.model.OrderRequest2Result;
import ru.yandex.market.common.balance.xmlrpc.model.PayRequestResultStructure;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;


/**
 * Тесты {@link AutoPaymentExecutor}
 */
@DbUnitDataSet(before = "csv/AutoPaymentTest.before.csv")
public class AutoPaymentTest extends FunctionalTest {
    @Autowired
    @Qualifier("patientBalanceService")
    BalanceService balanceService;
    @Autowired
    private AutoPaymentExecutor autoPaymentExecutor;

    private static final long UID = 887308675L;
    private static final long CLIENT_ID = 325076;
    private static final int REQUEST_ID = 12345;
    private static final String TRANSACTION_ID = "TRANSACTION_FOR_A_LOT_OF_MONEY";

    @BeforeEach
    void init() {
        var orderRequest2Result = new OrderRequest2Result(Map.of(OrderRequest2Result.FIELD_REQUEST_ID, REQUEST_ID));
        when(balanceService.createRequest2(eq(CLIENT_ID), anyList(), eq(UID)))
                .thenReturn(orderRequest2Result);

        when(balanceService.payRequest(eq(UID), any()))
                .thenReturn(new PayRequestResultStructure(Map.of(
                        PayRequestResultStructure.FIELD_TRANSACTION_ID, TRANSACTION_ID
                )));

        when(balanceService.getClients(eq(List.of(CLIENT_ID))))
                .thenReturn(Map.of(CLIENT_ID, new ClientInfo(CLIENT_ID, ClientType.PHYSICAL)));
    }

    void mockCheck(String responseCode) {
        PayRequestResultStructure resultStructure = new PayRequestResultStructure(Map.of(
                PayRequestResultStructure.FIELD_RESP_CODE, responseCode
        ));
        when(balanceService.checkRequestPayment(eq(UID), any()))
                .thenReturn(resultStructure);
    }

    void mockPayRequestWithException(BalanceException balanceException) {
        when(balanceService.payRequest(eq(UID), any()))
                .thenThrow(balanceException);
    }

    void mockPayRequestNoMethods() {
        final BalanceException balanceException =
                new BalanceException("<msg>" + BalanceException.NO_PAYMENT_OPTIONS_AVAILABLE + "</msg>");
        mockPayRequestWithException(balanceException);
    }

    void mockPayRequestPermissionDenied() {
        final BalanceException balanceException = new BalanceException("<code>PERMISSION_DENIED</code>");
        mockPayRequestWithException(balanceException);
    }

    void mockCheckNoPayments() {
        when(balanceService.checkRequestPayment(eq(UID), any()))
                .thenThrow(new BalanceException("<code>" + BalanceException.NO_PAYMENTS_FOR_REQUEST + "</code>"));
    }

    /**
     * Нет истории, первое автопополнение, все успешно.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "csv/AutoPaymentTest.autoPaymentExecutorTest.before.csv",
                    "csv/AutoPaymentTest.autoPaymentSettings.csv"
            },
            after = "csv/AutoPaymentTest.autoPaymentExecutorTest.after.csv")
    void autoPaymentExecutorTest() {
        mockCheck(PayRequestResultStructure.SUCCESS_RESPONSE_CODE);
        autoPaymentExecutor.doJob(null);
    }

    /**
     * Карта была отвязана.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "csv/AutoPaymentTest.autoPaymentExecutorTest.before.csv",
                    "csv/AutoPaymentTest.autoPaymentSettings.csv"
            },
            after = "csv/AutoPaymentTest.cardWasUnbindTest.after.csv")
    void cardWasUnbindTest() {
        mockPayRequestNoMethods();
        autoPaymentExecutor.doJob(null);
    }

    /**
     * В течении последних 24 часов уже была успешная попытка авто платежа.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "csv/AutoPaymentTest.alreadyPaidTest.before.csv",
                    "csv/AutoPaymentTest.autoPaymentSettings.csv"
            },
            after = "csv/AutoPaymentTest.alreadyPaidTest.before.csv")
    void alreadyPaidTest() {
        autoPaymentExecutor.doJob(null);
    }

    /**
     * В течении последних 24 часов была не успешная попытка авто платежа, нужно совершить еще одну.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "csv/AutoPaymentTest.lastTryWasFailedTest.before.csv",
                    "csv/AutoPaymentTest.autoPaymentSettings.csv"
            },
            after = "csv/AutoPaymentTest.lastTryWasFailedTest.after.csv")
    void lastTryWasFailedTest() {
        mockCheck(PayRequestResultStructure.SUCCESS_RESPONSE_CODE);
        autoPaymentExecutor.doJob(null);
    }

    /**
     * В течении последних 24 часов создали реквест, он успешен, но мы не знаем об этом.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "csv/AutoPaymentTest.lastTryWasSuccessButWeDontKnowTest.before.csv",
                    "csv/AutoPaymentTest.autoPaymentSettings.csv"
            },
            after = "csv/AutoPaymentTest.lastTryWasSuccessButWeDontKnowTest.after.csv")
    void lastTryWasSuccessButWeDontKnowTest() {
        mockCheck(PayRequestResultStructure.SUCCESS_RESPONSE_CODE);
        autoPaymentExecutor.doJob(null);
    }

    /**
     * В течении последних 24 часов создали реквест, но не стали за него платить.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "csv/AutoPaymentTest.lastTryWasntPaid.before.csv",
                    "csv/AutoPaymentTest.autoPaymentSettings.csv"
            },
            after = "csv/AutoPaymentTest.lastTryWasntPaid.after.csv")
    void lastTryWasntPaid() {
        mockCheckNoPayments();
        autoPaymentExecutor.doJob(null);
    }

    /**
     * Нет настроек, авто платеж не должен триггериться.
     */
    @Test
    @DbUnitDataSet(
            before = "csv/AutoPaymentTest.noSettingsTest.before.csv",
            after = "csv/AutoPaymentTest.noPayments.csv")
    void noSettingsTest() {
        autoPaymentExecutor.doJob(null);
    }

    private static Stream<String> notificationCodes() {
        return AutoPaymentService.CODE_WITH_NOTIFICATIONS.stream();
    }

    @DbUnitDataSet(before = "csv/AutoPaymentTest.notificationTest.before.csv")
    @ParameterizedTest(name = "Response code {0}")
    @MethodSource("notificationCodes")
    void notificationTest(String code) {
        mockCheck(code);
        autoPaymentExecutor.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 1, 1605793599L);
    }

    /**
     * Если кампания выключена, то надо выключить настройки.
     */
    @Test
    @DbUnitDataSet(before = "csv/AutoPaymentTest.disableSettingsIfCampaignDisabled.before.csv",
            after = "csv/AutoPaymentTest.disableSettingsIfCampaignDisabled.after.csv")
    void disableSettingsIfCampaignDisabled() {
        autoPaymentExecutor.doJob(null);
    }

    /**
     * Если нет доступа у пользователя, то надо выключить настройки.
     */
    @Test
    @DbUnitDataSet(before = "csv/AutoPaymentTest.disableSettingsIfPermissionDenied.before.csv",
            after = "csv/AutoPaymentTest.disableSettingsIfPermissionDenied.after.csv")
    void disableSettingsIfPermissionDenied() {
        mockPayRequestPermissionDenied();
        autoPaymentExecutor.doJob(null);
    }
}
