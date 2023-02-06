package ru.yandex.market.core.partner.onboarding.state;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerOnboardingStateParamNamespaceTest {
    @Test
    void makeFullParamName() {
        assertThat(PartnerOnboardingStateParamNamespace.COMMON.makeFullParamName("pn"))
                .isEqualTo("common.pn");
        assertThat(PartnerOnboardingStateParamNamespace.UNKNOWN.makeFullParamName("pn"))
                .isEqualTo("pn");
    }

    @Test
    void tryParseFullParamName() {
        var random = "3E24E829-1FDF-4E2A-8D34-F9EBA4B196A7.pn"; // unknown prefix, has delimeter
        assertThat(PartnerOnboardingStateParamNamespace.tryParseFullParamName("common.pn"))
                .isEqualTo(Pair.of(PartnerOnboardingStateParamNamespace.COMMON, "pn"));
        assertThat(PartnerOnboardingStateParamNamespace.tryParseFullParamName("pn"))
                .isEqualTo(Pair.of(PartnerOnboardingStateParamNamespace.UNKNOWN, "pn"));
        assertThat(PartnerOnboardingStateParamNamespace.tryParseFullParamName(random))
                .isEqualTo(Pair.of(PartnerOnboardingStateParamNamespace.UNKNOWN, random));
    }
}
