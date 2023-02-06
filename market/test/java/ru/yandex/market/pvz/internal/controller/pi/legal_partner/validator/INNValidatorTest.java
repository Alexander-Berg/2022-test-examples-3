package ru.yandex.market.pvz.internal.controller.pi.legal_partner.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pvz.client.model.partner.LegalForm;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerType;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRepository;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
public class INNValidatorTest {

    @Mock
    private LegalPartnerRepository legalPartnerRepository;
    @InjectMocks
    private INNValidator validator;

    @Test
    void validTaxpayerNumber() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)
                .taxpayerNumber("7704407589")
                .kpp("399401234")
                .ogrn("315774500419700")
                .build();

        assertThat(validator.isValid(organization, null)).isTrue();
    }

    @Test
    void validTaxpayerNumber12() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)
                .taxpayerNumber("772815514319")
                .kpp("399401234")
                .ogrn("315774500419700")
                .build();

        assertThat(validator.isValid(organization, null)).isTrue();
    }

    @Test
    void invalidTaxpayerNumberLength() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)
                .taxpayerNumber("77044075877")
                .kpp("399401234")
                .ogrn("315774500419700")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }

    @Test
    void invalidTaxpayerNumberFormat() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)
                .taxpayerNumber("770440758d")
                .kpp("399401234")
                .ogrn("315774500419700")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }

    @Test
    void invalidTaxpayerNumberFormatForLegalPerson() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.LEGAL_PERSON)
                .legalForm(LegalForm.OOO)
                .taxpayerNumber("772815514319")
                .kpp("399401234")
                .ogrn("3157745004197")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }

    @Test
    void invalidTaxpayerNumberChecksum() {
        OrganizationDto organization = OrganizationDto.builder()
                .legalType(LegalPartnerType.INDIVIDUAL_ENTREPRENEURSHIP)
                .taxpayerNumber("7704407588")
                .kpp("399401234")
                .ogrn("315774500419700")
                .build();

        assertThat(validator.isValid(organization, null)).isFalse();
    }
}
