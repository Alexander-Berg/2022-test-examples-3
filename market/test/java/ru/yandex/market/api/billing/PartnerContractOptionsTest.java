package ru.yandex.market.api.billing;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PartnerContractOptionsTest {

    @Test
    public void testPayoutFrequencyIsSet() {
        PartnerContractOptionWithFrequency option1 = new PartnerContractOptionWithFrequency(
                1000L,
                "Contract 1",
                PayoutFrequency.DAILY,
                false,
                PayoutFrequency.WEEKLY
        );
        PartnerContractOptionWithFrequency option2 = new PartnerContractOptionWithFrequency(
                2000L,
                "Contract 1",
                PayoutFrequency.DAILY,
                true,
                PayoutFrequency.WEEKLY
        );

        PartnerContractOptionsWithFrequency options = new PartnerContractOptionsWithFrequency(1000L, List.of(option1, option2));

        assertThat(options.isPaymentFrequencySetForCurrentContract()).isTrue();
    }

    @Test
    public void testPayoutFrequencyIsNotSet() {
        PartnerContractOptionWithFrequency option1 = new PartnerContractOptionWithFrequency(
                1000L,
                "Contract 1",
                PayoutFrequency.DAILY,
                false,
                PayoutFrequency.WEEKLY
        );
        PartnerContractOptionWithFrequency option2 = new PartnerContractOptionWithFrequency(
                2000L,
                "Contract 1",
                PayoutFrequency.DAILY,
                true,
                PayoutFrequency.WEEKLY
        );

        PartnerContractOptionsWithFrequency options = new PartnerContractOptionsWithFrequency(2000L, List.of(option1, option2));

        assertThat(options.isPaymentFrequencySetForCurrentContract()).isFalse();
    }

    @Test
    public void testPayoutFrequencyOnNull() {
        PartnerContractOptionWithFrequency option1 = new PartnerContractOptionWithFrequency(
                1000L,
                "Contract 1",
                PayoutFrequency.DAILY,
                null,
                PayoutFrequency.WEEKLY
        );
        PartnerContractOptionWithFrequency option2 = new PartnerContractOptionWithFrequency(
                2000L,
                "Contract 1",
                PayoutFrequency.DAILY,
                true,
                PayoutFrequency.WEEKLY
        );

        PartnerContractOptionsWithFrequency options = new PartnerContractOptionsWithFrequency(1000L, List.of(option1, option2));

        assertThat(options.isPaymentFrequencySetForCurrentContract()).isFalse();
    }
}
