package ru.yandex.market.checkout.providers;

import ru.yandex.market.checkout.checkouter.balance.service.BalanceTokenProvider;
import ru.yandex.market.checkout.checkouter.balance.service.BlueBalanceTokenProvider;

public final class BalanceTokenProviderProvider {

    public static final String SERVICE_TOKEN_FOR_YAD = "serviceTokenForYad";
    public static final String SERVICE_TOKEN_FOR_MARKET = "serviceTokenForMarket";
    public static final String SERVICE_TOKEN_FOR_CASH = "serviceTokenForCash";
    public static final String SERVICE_TOKEN_FOR_BLUE_PAY = "serviceTokenForBluePay";
    public static final String SERVICE_TOKEN_FOR_BLUE_SUBSIDY = "serviceTokenForBlueSubsidy";
    private static final String SERVICE_TOKEN_FOR_COMPENSATIONS = "serviceTokenForCompensations";
    private static final String SERVICE_TOKEN_FOR_EXTENAL_PAYMENT = "serviceTokenForExtenalPayment";

    private BalanceTokenProviderProvider() {
    }

    public static BalanceTokenProvider generateProvider() {
        BlueBalanceTokenProvider balanceTokenProvider = new BlueBalanceTokenProvider();
        balanceTokenProvider.setServiceTokenIdForYad(119L);
        balanceTokenProvider.addToken(balanceTokenProvider.getServiceTokenIdForYad(), SERVICE_TOKEN_FOR_YAD);

        balanceTokenProvider.setServiceTokenIdForMarket(172L);
        balanceTokenProvider.addToken(balanceTokenProvider.getServiceTokenIdForMarket(), SERVICE_TOKEN_FOR_MARKET);

        balanceTokenProvider.setServiceTokenIdForCash(603L);
        balanceTokenProvider.addToken(balanceTokenProvider.getServiceTokenIdForCash(), SERVICE_TOKEN_FOR_CASH);

        balanceTokenProvider.setServiceTokenIdForPay(610L);
        balanceTokenProvider.addToken(balanceTokenProvider.getServiceTokenIdForPay(), SERVICE_TOKEN_FOR_BLUE_PAY);

        balanceTokenProvider.setServiceTokenIdForSubsidy(609L);
        balanceTokenProvider.addToken(
                balanceTokenProvider.getServiceTokenIdForSubsidy(),
                SERVICE_TOKEN_FOR_BLUE_SUBSIDY
        );
        balanceTokenProvider.setServiceTokenIdForCompensations(613L);
        balanceTokenProvider.addToken(
                balanceTokenProvider.getServiceTokenIdForCompensations(),
                SERVICE_TOKEN_FOR_COMPENSATIONS
        );

        balanceTokenProvider.setServiceTokenIdForExternalPayment(633L);
        balanceTokenProvider.addToken(
                balanceTokenProvider.getServiceTokenIdForExternalPayment(),
                SERVICE_TOKEN_FOR_EXTENAL_PAYMENT);
        return balanceTokenProvider;
    }
}
