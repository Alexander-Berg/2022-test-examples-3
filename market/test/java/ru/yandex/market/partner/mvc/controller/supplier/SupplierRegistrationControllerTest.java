package ru.yandex.market.partner.mvc.controller.supplier;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.orginfo.model.OrganizationInfo;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.id.LegalInfo;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierRegistrationControllerTest {
    @Test
    void makeOrganizationInfoDTOEmpty() {
        // when
        var result = SupplierRegistrationController.makeOrganizationInfoDTO(
                Optional.empty(),
                Optional.empty()
        );

        // then
        assertThat(result.getFactAddress()).isNull();
        assertThat(result.getInn()).isNull();
        assertThat(result.getJuridicalAddress()).isNull();
        assertThat(result.getName()).isNull();
        assertThat(result.getOgrn()).isNull();
        assertThat(result.getType()).as("we should not guess defaults").isNull();
    }

    @Test
    void makeOrganizationInfoDTOFallback() {
        // given
        var legalInfoFallback = new OrganizationInfo();
        legalInfoFallback.setJuridicalAddress("JA");
        legalInfoFallback.setName("N");
        legalInfoFallback.setOgrn("OGRN");
        legalInfoFallback.setType(OrganizationType.OAO);

        // when
        var result = SupplierRegistrationController.makeOrganizationInfoDTO(
                Optional.empty(),
                Optional.of(legalInfoFallback)
        );

        // then
        assertThat(result.getInn()).as("fallback info does not have INN").isNull();
        assertThat(result.getJuridicalAddress()).isEqualTo(legalInfoFallback.getJuridicalAddress());
        assertThat(result.getName()).isEqualTo(legalInfoFallback.getName());
        assertThat(result.getOgrn()).isEqualTo(legalInfoFallback.getOgrn());
        assertThat(result.getType()).isEqualTo(legalInfoFallback.getType());
    }

    @Test
    void makeOrganizationInfoDTO() {
        // given
        var legalInfo = LegalInfo.newBuilder()
                .setInn("INN")
                .setLegalAddress("JA")
                .setLegalName("N")
                .setPhysicalAddress("FA")
                .setRegistrationNumber("OGRN")
                .setType("ooo") // it might be of values out of enum range and set externally
                .build();

        var legalInfoFallback = new OrganizationInfo();
        legalInfoFallback.setName("N2");
        legalInfoFallback.setOgrn("OGRN2");
        legalInfoFallback.setType(OrganizationType.OAO);

        // when
        var result = SupplierRegistrationController.makeOrganizationInfoDTO(
                Optional.of(legalInfo),
                Optional.of(legalInfoFallback)
        );

        // then
        assertThat(result.getInn()).isEqualTo(legalInfo.getInn());
        assertThat(result.getJuridicalAddress()).isEqualTo(legalInfo.getLegalAddress());
        assertThat(result.getName()).isEqualTo(legalInfo.getLegalName());
        assertThat(result.getOgrn()).isEqualTo(legalInfo.getRegistrationNumber());
        assertThat(result.getType()).as("legal info info is preferred if possible").isEqualTo(OrganizationType.OOO);
    }
}
