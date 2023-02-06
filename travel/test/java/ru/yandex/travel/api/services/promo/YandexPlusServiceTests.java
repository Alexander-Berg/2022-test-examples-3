package ru.yandex.travel.api.services.promo;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.opentracing.mock.MockTracer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.orders.services.payments.TrustClient;
import ru.yandex.travel.orders.services.payments.model.TrustBoundPaymentMethod;
import ru.yandex.travel.orders.services.payments.model.TrustBoundPaymentMethodType;
import ru.yandex.travel.orders.services.payments.model.TrustPaymentMethodsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class YandexPlusServiceTests {
    private static final String PASSPORT_ID = "123";
    private static final String USER_IP = "1.1.1.1";
    private static final ProtoCurrencyUnit CURRENCY = ProtoCurrencyUnit.RUB;

    private TrustClient trustClient;
    private YandexPlusService yandexPlusService;

    @Before
    public void init() {
        trustClient = Mockito.mock(TrustClient.class);
        yandexPlusService = new YandexPlusService(trustClient, new Retry(new MockTracer()));
    }

    private void mockTrustBalance(int balance) {
        when(trustClient.getPaymentMethodsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(TrustPaymentMethodsResponse.builder()
                        .boundPaymentMethods(List.of(TrustBoundPaymentMethod.builder()
                                .id(PASSPORT_ID)
                                .paymentMethod(TrustBoundPaymentMethodType.YANDEX_ACCOUNT)
                                .balance(BigDecimal.valueOf(balance))
                                .currency("RUB")
                                .build()))
                        .build()));
    }

    @Test
    public void getYandexPlusBalance_ok() {
        mockTrustBalance(100);

        CompletableFuture<Integer> balanceFuture = yandexPlusService.getYandexPlusBalance(
                PASSPORT_ID, USER_IP, CURRENCY);
        Integer balance = balanceFuture.join();
        assertThat(balance).isEqualTo(100);
    }

    @Test(expected = RuntimeException.class)
    public void getYandexPlusBalance_trustUnavailable() {
        when(trustClient.getPaymentMethodsAsync(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test: Trust is on fire")));

        yandexPlusService.getYandexPlusBalance(PASSPORT_ID, USER_IP, CURRENCY).join();
    }

    @Test
    public void getYandexPlusBalanceWithoutException_ok() {
        mockTrustBalance(100);

        CompletableFuture<Integer> balanceFuture = yandexPlusService.getYandexPlusBalanceWithoutException(
                PASSPORT_ID, USER_IP, CURRENCY);
        Integer balance = balanceFuture.join();
        assertThat(balance).isEqualTo(100);
    }

    @Test
    public void getYandexPlusBalanceWithoutException_trustUnavailable() {
        when(trustClient.getPaymentMethodsAsync(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test: Trust is on fire")));

        CompletableFuture<Integer> balanceFuture = yandexPlusService.getYandexPlusBalanceWithoutException(
                PASSPORT_ID, USER_IP, CURRENCY);
        Integer balance = balanceFuture.join();
        assertThat(balance).isNull();
    }
}
