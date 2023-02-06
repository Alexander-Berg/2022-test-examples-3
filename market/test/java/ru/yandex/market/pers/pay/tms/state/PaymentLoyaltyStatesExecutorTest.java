package ru.yandex.market.pers.pay.tms.state;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualRequest;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualRequestList;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualResponse;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualResponseList;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualStatus;
import ru.yandex.market.loyalty.api.model.perk.PerkStat;
import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayState;
import ru.yandex.market.pers.pay.service.TmsPaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.pay.tms.state.PaymentLoyaltyStatesExecutor.LOYALTY_REQUEST_BATCH_SIZE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.03.2021
 */
public class PaymentLoyaltyStatesExecutorTest extends AbstractPaymentStatesExecutorTest {

    @Autowired
    private MarketLoyaltyClient loyaltyClient;

    @Autowired
    private TmsPaymentService paymentService;

    @Autowired
    private PaymentContentStatesExecutor executorState;
    @Autowired
    private PaymentLoyaltyStatesExecutor executorLoyalty;

    @Autowired
    private ComplexMonitoring complexMonitoring;

    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @Captor
    private ArgumentCaptor<YandexWalletAccrualRequestList> requestCaptor;

    @Test
    public void testLoyaltyCall() {
        mockPlusAccountCheck();

        // loyalty accepts all payments
        when(loyaltyClient.accrual(any())).then(invocation -> {
            YandexWalletAccrualRequestList requestList = invocation.getArgument(0);
            return new YandexWalletAccrualResponseList(requestList.getData().stream()
                .map(req -> {
                    Map<String, String> expectedPayload = Map.of(
                        "cashback_service", "market",
                        "cashback_type", "nontransaction",
                        "service_id", "663",
                        "has_plus", req.getUid() % 2 == 0 ? "true" : "false",
                        "issuer", "perspay",
                        "campaign_name", "ugc.reviews",
                        "product_id", "ugc_market_review_reward"
                    );

                    assertEquals(expectedPayload, req.getPayload());

                    return new YandexWalletAccrualResponse(
                        req.getCampaignName(),
                        req.getReferenceId(),
                        YandexWalletAccrualStatus.SUCCESS
                    );
                })
                .collect(Collectors.toList())
            );
        });

        // create payments
        // check all are executed
        long payId = createTestPayment(MODEL_ID, USER_ID);
        long payId2 = createTestPayment(MODEL_ID, USER_ID + 1);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.APPROVED);

        executorState.processPaymentContentStates();
        executorLoyalty.processPaymentLoyaltyCall();

        verify(loyaltyClient, times(1)).accrual(requestCaptor.capture());

        YandexWalletAccrualRequestList requestList = requestCaptor.getValue();
        assertEquals(2, requestList.getData().size());

        assertEquals(Set.of(USER_ID, USER_ID + 1),
            requestList.getData().stream().map(YandexWalletAccrualRequest::getUid).collect(Collectors.toSet()));

        requestList.getData().forEach(item -> {
            assertEquals(PaymentLoyaltyStatesExecutor.PRODUCT_ID_PAID_REVIEW, item.getProductId());
        });

        assertState(payId, PersPayState.PAYMENT_SENT);
        assertState(payId2, PersPayState.PAYMENT_SENT);

        verify(complexMonitoring, times(0)).addTemporaryWarning(any(), any(), anyLong(), any());
    }

    @Test
    public void testLoyaltyCallBatchedRequest() {
        mockPlusAccountCheck();

        // loyalty accepts all payments
        when(loyaltyClient.accrual(any())).then(invocation -> {
            YandexWalletAccrualRequestList requestList = invocation.getArgument(0);
            return new YandexWalletAccrualResponseList(requestList.getData().stream()
                .map(req -> {
                    Map<String, String> expectedPayload = Map.of(
                        "cashback_service", "market",
                        "cashback_type", "nontransaction",
                        "service_id", "663",
                        "has_plus", req.getUid() % 2 == 0 ? "true" : "false",
                        "issuer", "perspay",
                        "campaign_name", "ugc.reviews",
                        "product_id", "ugc_market_review_reward"
                    );

                    assertEquals(expectedPayload, req.getPayload());

                    return new YandexWalletAccrualResponse(
                        req.getCampaignName(),
                        req.getReferenceId(),
                        YandexWalletAccrualStatus.SUCCESS
                    );
                })
                .collect(Collectors.toList())
            );
        });

        List<Long> payIds = IntStream.range(0, LOYALTY_REQUEST_BATCH_SIZE + 3).boxed()
            .map(x -> {
                long payId = createTestPayment(MODEL_ID, USER_ID + x);
                createModelGrade(MODEL_ID, USER_ID + x, PersPayEntityState.APPROVED);
                return payId;
            })
            .collect(Collectors.toList());

        executorState.processPaymentContentStates();
        executorLoyalty.processPaymentLoyaltyCall();

        verify(loyaltyClient, times(2)).accrual(requestCaptor.capture());

        payIds.forEach(id -> assertState(id, PersPayState.PAYMENT_SENT));

        verify(complexMonitoring, times(0)).addTemporaryWarning(any(), any(), anyLong(), any());
    }

    @Test
    public void testFailedPayments() {
        // create payments
        // check all are executed
        long payId = createTestPayment(MODEL_ID, USER_ID);
        long payId2 = createTestPayment(MODEL_ID, USER_ID + 1);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.APPROVED);

        mockPlusAccountCheck();

        when(loyaltyClient.accrual(any())).then(invocation -> {
            YandexWalletAccrualRequestList requestList = invocation.getArgument(0);
            return new YandexWalletAccrualResponseList(requestList.getData().stream()
                .map(req -> {
                    return new YandexWalletAccrualResponse(
                        req.getCampaignName(),
                        req.getReferenceId(),
                        req.getReferenceId().equals(String.valueOf(payId))
                            ? YandexWalletAccrualStatus.SUCCESS
                            : YandexWalletAccrualStatus.UNKNOWN
                    );
                })
                .collect(Collectors.toList())
            );
        });

        executorState.processPaymentContentStates();
        executorLoyalty.processPaymentLoyaltyCall();

        assertState(payId, PersPayState.PAYMENT_SENT);
        assertState(payId2, PersPayState.NEED_PAY);

        verify(complexMonitoring, times(1)).addTemporaryWarning(any(), any(), anyLong(), any());
    }

    @Test
    public void testStatusCheck() {
        long payOk = createTestPayment(MODEL_ID, USER_ID);
        long payHang = createTestPayment(MODEL_ID, USER_ID+1);
        long payBad = createTestPayment(MODEL_ID, USER_ID+2);

        paymentService.changeState(List.of(payOk, payBad, payHang), PersPayState.PAYMENT_SENT, Map.of());

        long payNotReady = createTestPayment(MODEL_ID, USER_ID+3);

        when(yqlJdbcTemplate.queryForList(any(String.class), eq(Long.class))).thenReturn(List.of(payOk, payNotReady));

        executorLoyalty.checkLoyaltyPaymentState();

        assertState(payOk, PersPayState.PAYED);
        assertState(payHang, PersPayState.PAYMENT_SENT);
        assertState(payBad, PersPayState.PAYMENT_SENT);
        assertState(payNotReady, PersPayState.NEW);
    }

    private void mockPlusAccountCheck() {
        // all users mod2 has plus account
        when(loyaltyClient.perkStatus(any(PerkType.class), anyLong(), anyLong(), anyBoolean())).then(
            invocation -> {
                long uid = invocation.getArgument(1);
                return PerkStatResponse.builder()
                    .addPerkStat(PerkStat.builder().setType(PerkType.YANDEX_PLUS).setPurchased(uid % 2 == 0).build())
                    .build();
            }
        );
    }

}
