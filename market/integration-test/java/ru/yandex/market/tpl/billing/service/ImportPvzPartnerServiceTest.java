package ru.yandex.market.tpl.billing.service;

import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerDto;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ImportPvzPartnerServiceTest extends AbstractFunctionalTest {

    @Autowired
    private PvzClient pvzClient;

    @Autowired
    private ImportPvzPartnerService importPvzPartnerService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(pvzClient);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpartnerservice/before/no_changes.csv",
            after = "/database/service/importpartnerservice/before/no_changes.csv")
    @DisplayName("передается пустой лист вместо партнеров")
    void importPartnersTestEmptyList() {
        when(pvzClient.getLegalPartners()).thenReturn(List.of());
        importPvzPartnerService.importPartners();
        verify(pvzClient).getLegalPartners();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpartnerservice/before/no_changes.csv",
            after = "/database/service/importpartnerservice/before/no_changes.csv")
    @DisplayName("передан 1 невалидный партнер")
    void importPartnersTestOnePartner() {
        when(pvzClient.getLegalPartners()).thenReturn(List.of(BillingLegalPartnerDto
                .builder()
                .balanceClientId(12)
                .legalPartnerId(1)
                .build()));
        Assertions.assertThatThrownBy(importPvzPartnerService::importPartners)
                .isInstanceOf(TplIllegalStateException.class)
                .hasMessage("Some partners have wrong balance_client_id partner_legal_id's: 1 total: 1");
        verify(pvzClient).getLegalPartners();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpartnerservice/before/no_changes.csv",
            after = "/database/service/importpartnerservice/before/no_changes.csv")
    @DisplayName("передано 11 невалидных партнеров")
    void importPartnersTestMoreThenTopWrongPartners() {
        when(pvzClient.getLegalPartners()).thenReturn(getDtosMoreThenTopWrongPartners());
        Assertions.assertThatThrownBy(importPvzPartnerService::importPartners)
                .isInstanceOf(TplIllegalStateException.class)
                .hasMessage("Some partners have wrong balance_client_id partner_legal_id's: 1,2,3,4,5,6,7,8,9,10 " +
                        "total: 11");
        verify(pvzClient).getLegalPartners();
    }

    @Test
    @DbUnitDataSet(after = "/database/service/importpartnerservice/after/new_partners.csv")
    @DisplayName("переданы новые партнеры")
    void importPartnersTestWithNewPartners() {
        when(pvzClient.getLegalPartners()).thenReturn(getDtosNewPartners());
        importPvzPartnerService.importPartners();
        verify(pvzClient).getLegalPartners();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpartnerservice/before/update_partners.csv",
            after = "/database/service/importpartnerservice/after/update_partners.csv")
    @DisplayName("обновление партнеров")
    void importPartnersTestUpdatePartners() {
        when(pvzClient.getLegalPartners()).thenReturn(getDtosUpdatePartner());
        importPvzPartnerService.importPartners();
        verify(pvzClient).getLegalPartners();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/importpartnerservice/before/update_and_save_partners.csv",
            after = "/database/service/importpartnerservice/after/update_and_save_partners.csv")
    @DisplayName("обновление и сохранение новых партнеров")
    void importPartnersTestUpdateAndInsertPartners() {
        when(pvzClient.getLegalPartners()).thenReturn(getDtosUpdatePartnerAndSave());
        importPvzPartnerService.importPartners();
        verify(pvzClient).getLegalPartners();
    }

    @Test
    @DbUnitDataSet(after = "/database/service/importpartnerservice/after/save_patner_with_contract_number.csv")
    @DisplayName("партнеры без номеров контрактов")
    void partnersWithoutContractNumber() {
        when(pvzClient.getLegalPartners()).thenReturn(getDtosPartnersWithoutContractNumber());
        importPvzPartnerService.importPartners();
        verify(pvzClient).getLegalPartners();
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosMoreThenTopWrongPartners() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .balanceClientId(11)
                        .legalPartnerId(1)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(12)
                        .legalPartnerId(2)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(13)
                        .legalPartnerId(3)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(14)
                        .legalPartnerId(4)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(15)
                        .legalPartnerId(5)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(16)
                        .legalPartnerId(6)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(17)
                        .legalPartnerId(7)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(18)
                        .legalPartnerId(8)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(19)
                        .legalPartnerId(9)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(20)
                        .legalPartnerId(10)
                        .build(),
                BillingLegalPartnerDto.builder()
                        .balanceClientId(21)
                        .legalPartnerId(11)
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosNewPartners() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(1)
                        .balanceClientId(1)
                        .legalPartnerId(1)
                        .virtualAccountNumber("1")
                        .inn("1")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651537")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(2)
                        .balanceClientId(2)
                        .legalPartnerId(2)
                        .virtualAccountNumber("2")
                        .inn("2")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651536")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(3)
                        .balanceClientId(3)
                        .legalPartnerId(3)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651535")
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosUpdatePartner() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(1)
                        .balanceClientId(1)
                        .legalPartnerId(1)
                        .virtualAccountNumber("1")
                        .inn("1123")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651535")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(2)
                        .balanceClientId(2)
                        .legalPartnerId(2)
                        .virtualAccountNumber("2")
                        .inn("223")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651536")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(3)
                        .balanceClientId(3)
                        .legalPartnerId(3)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651537")
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosUpdatePartnerAndSave() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(1)
                        .balanceClientId(1)
                        .legalPartnerId(1)
                        .virtualAccountNumber("1")
                        .inn("222")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651535")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(2)
                        .balanceClientId(2)
                        .legalPartnerId(2)
                        .virtualAccountNumber("2")
                        .inn("234")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651536")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(3)
                        .balanceClientId(3)
                        .legalPartnerId(3)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651537")
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosPartnersWithoutContractNumber() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(1)
                        .balanceClientId(1)
                        .legalPartnerId(1)
                        .virtualAccountNumber("1")
                        .inn("1")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(2)
                        .balanceClientId(2)
                        .legalPartnerId(2)
                        .virtualAccountNumber("2")
                        .inn("2")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(3)
                        .balanceClientId(3)
                        .legalPartnerId(3)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .contractNumber("ДЛ-1651535")
                        .build()
        );
    }
}
