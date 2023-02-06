package ru.yandex.market.pvz.internal.controller.pi.legal_partner.validator;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.client.model.partner.LegalForm;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerType;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

class LegalFormValidatorTest {

    private final LegalFormValidator validator = new LegalFormValidator();

    @Test
    void validLegalFormForLegalPerson() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.LEGAL_PERSON)
                .legalForm(LegalForm.OAO)
                .taxpayerNumber("7704407589")
                .kpp("399401234")
                .ogrn("3157745004197")
                .build();

        assertThat(validator.isValid(organization, null)).isTrue();
    }

    @Test
    void invalidLegalFormForLegalPerson() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.LEGAL_PERSON)
                .taxpayerNumber("7704407589")
                .kpp("399401234")
                .ogrn("315774500419700")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }
}
