package ru.yandex.market.billing.factoring.matcher;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.factoring.model.ContractFactor;
import ru.yandex.market.core.payment.PaymentOrderFactoring;
import ru.yandex.market.mbi.util.MbiMatchers;

public class ContractFactorMatcher {

    private ContractFactorMatcher() {
    }

    public static Matcher<ContractFactor> hasContractId(Long expectedValue) {
        return MbiMatchers.<ContractFactor>newAllOfBuilder()
                .add(ContractFactor::getContractId, expectedValue, "contractId")
                .build();
    }

    public static Matcher<ContractFactor> hasFactor(PaymentOrderFactoring expectedValue) {
        return MbiMatchers.<ContractFactor>newAllOfBuilder()
                .add(ContractFactor::getFactor, expectedValue, "factor")
                .build();
    }
}
