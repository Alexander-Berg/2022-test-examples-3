package ru.yandex.market.pvz.internal.controller.pi.legal_partner.validator;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.client.model.partner.LegalPartnerType;

import static org.assertj.core.api.Assertions.assertThat;

class KPPValidatorTest {

    private final KPPValidator validator = new KPPValidator();

    @Test
    void validKpp() {
        assertThat(validator.isValid("399401234", LegalPartnerType.LEGAL_PERSON)).isTrue();
    }

    @Test
    void invalidKppLength() {
        assertThat(validator.isValid("3994012349", LegalPartnerType.LEGAL_PERSON)).isFalse();
    }

    @Test
    void invalidKppFormat() {
        assertThat(validator.isValid("399401234d", LegalPartnerType.LEGAL_PERSON)).isFalse();
    }

    @Test
    void notCheckKppForIndividualEntrepreneurship() {
        assertThat(validator.isValid(null, LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)).isTrue();
    }

}
