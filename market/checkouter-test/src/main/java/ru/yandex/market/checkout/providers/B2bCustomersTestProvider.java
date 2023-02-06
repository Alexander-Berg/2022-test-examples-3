package ru.yandex.market.checkout.providers;

import java.util.List;
import java.util.Set;

import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.B2B_ACCOUNT_PREPAYMENT;

public final class B2bCustomersTestProvider {

    public static final long BUSINESS_BALANCE_ID = 111L;
    public static final long CONTRACT_ID = 222L;

    private B2bCustomersTestProvider() {
    }

    public static Parameters defaultB2bParameters() {
        var parameters = BlueParametersProvider.prepaidBlueOrderParameters();
        parameters.configuration().cart().mockConfigurations().values().stream().findFirst()
                .get().getReportParameters().getActualDelivery().getResults().get(0).getDelivery().get(0)
                .setPaymentMethods(Set.of(B2B_ACCOUNT_PREPAYMENT.name()));
        parameters.configuration().cart().mockConfigurations().values().stream().findFirst()
                .get().getReportParameters().getActualDelivery().getResults().get(0).getPickup().get(0)
                .setPaymentMethods(Set.of(B2B_ACCOUNT_PREPAYMENT.name()));
        var dr = new DeliveryResponse();
        dr.setPaymentOptions(Set.of(B2B_ACCOUNT_PREPAYMENT));
        parameters.configuration().cart().mockConfigurations().values().stream().findFirst()
                .get().setPushApiDeliveryResponses(List.of(dr));
        parameters.setPaymentMethod(B2B_ACCOUNT_PREPAYMENT);
        parameters.getBuyer().setBusinessBalanceId(BUSINESS_BALANCE_ID);
        return parameters;
    }

}
