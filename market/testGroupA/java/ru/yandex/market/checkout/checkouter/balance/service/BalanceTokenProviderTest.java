package ru.yandex.market.checkout.checkouter.balance.service;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.providers.BalanceTokenProviderProvider;

public class BalanceTokenProviderTest extends AbstractServicesTestBase {

    private BalanceTokenProvider balanceTokenProvider;

    @BeforeEach
    public void beforeTest() {
        balanceTokenProvider = BalanceTokenProviderProvider.generateProvider();
    }

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{
                        PaymentGoal.ORDER_PREPAY,
                        PrepayType.YANDEX_MARKET,
                        BalanceTokenProviderProvider.SERVICE_TOKEN_FOR_BLUE_PAY
                },
                new Object[]{
                        PaymentGoal.ORDER_POSTPAY,
                        PrepayType.YANDEX_MARKET,
                        BalanceTokenProviderProvider.SERVICE_TOKEN_FOR_BLUE_PAY
                },
                new Object[]{
                        PaymentGoal.SUBSIDY,
                        PrepayType.YANDEX_MARKET,
                        BalanceTokenProviderProvider.SERVICE_TOKEN_FOR_BLUE_SUBSIDY
                }
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void getServiceTokenTest(PaymentGoal paymentGoal, PrepayType prepayType, String expectedToken)
            throws Exception {
        String serviceToken = balanceTokenProvider.getServiceToken(
                paymentGoal,
                prepayType
        );
        Assertions.assertEquals(expectedToken, serviceToken);
    }
}
