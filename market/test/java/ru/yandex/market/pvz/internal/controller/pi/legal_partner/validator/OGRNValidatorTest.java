package ru.yandex.market.pvz.internal.controller.pi.legal_partner.validator;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.client.model.partner.LegalForm;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerType;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

class OGRNValidatorTest {

    private final OGRNValidator validator = new OGRNValidator();

    @Test
    void validOgrn() {
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
    void invalidOgrnLength() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.LEGAL_PERSON)
                .legalForm(LegalForm.OAO)
                .taxpayerNumber("7704407589")
                .kpp("399401234")
                .ogrn("315774500419700")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }

    @Test
    void invalidOgrnFormat() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.LEGAL_PERSON)
                .legalForm(LegalForm.OAO)
                .taxpayerNumber("7704407589")
                .kpp("399401234")
                .ogrn("315774500419d")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }

    @Test
    void invalidOgrnipLength() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)
                .taxpayerNumber("7704407589")
                .kpp("399401234")
                .ogrn("31577450041970")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }

    @Test
    void invalidOgrnipFormat() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)
                .taxpayerNumber("7704407589")
                .kpp("399401234")
                .ogrn("31577450041970d")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }

}
