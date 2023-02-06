package ru.yandex.market.tpl.billing.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerDto;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;

class PartnerServiceTest extends AbstractFunctionalTest {

    @Autowired
    private PartnerService partnerService;

    @Test
    @DisplayName("передан пустой лист")
    void getCheckedPartnersByBalanceClientIdTestEmpty() {
        List<BillingLegalPartnerDto> actualResult = partnerService.getCheckedPartnersByBalanceClientId(List.of());
        Assertions.assertThat(actualResult).isEmpty();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/partnerservice/before/same_result.csv",
            after = "/database/service/partnerservice/after/same_result.csv")
    @DisplayName("передано 2 неликвидных партнера")
    void getCheckedPartnersByBalanceClientIdTestTwoPartnersIllegal() {
        List<BillingLegalPartnerDto> actualResult
                = partnerService.getCheckedPartnersByBalanceClientId(getDtosPartners());
        Assertions.assertThat(actualResult).isEqualTo(getDtosPartnersAnswer());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/partnerservice/before/same_result.csv",
            after = "/database/service/partnerservice/after/same_result.csv")
    @DisplayName("все партнеры неликвидные")
    void getCheckedPartnersByBalanceClientIdTestAllPartnersIllegal() {
        List<BillingLegalPartnerDto> actualResult =
                partnerService.getCheckedPartnersByBalanceClientId(getDtosPartnersAllIllegal());
        Assertions.assertThat(actualResult).isEqualTo(getDtosPartnersAllIllegal());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/partnerservice/before/same_result.csv",
            after = "/database/service/partnerservice/after/same_result.csv")
    @DisplayName("неликвидные партнеры и новые")
    void getCheckedPartnersByBalanceClientIdTestIllegalPartnersAndNewPartners() {
        List<BillingLegalPartnerDto> partnerDtos = new ArrayList<>();
        partnerDtos.addAll(getDtosPartnersAllIllegal());
        partnerDtos.addAll(getNewPartners());
        List<BillingLegalPartnerDto> actualResult =
                partnerService.getCheckedPartnersByBalanceClientId(partnerDtos);
        Assertions.assertThat(actualResult).isEqualTo(getDtosPartnersAllIllegal());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/partnerservice/before/same_result.csv",
            after = "/database/service/partnerservice/after/same_result.csv")
    @DisplayName("все партнеры ликвидные")
    void getCheckedPartnersByBalanceClientIdTestLegalPartners() {
        List<BillingLegalPartnerDto> actualResult =
                partnerService.getCheckedPartnersByBalanceClientId(getAllLegalPartners());
        Assertions.assertThat(actualResult).isEqualTo(List.of());
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosPartners() {
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
                        .balanceClientId(22)
                        .legalPartnerId(2)
                        .virtualAccountNumber("2")
                        .inn("2")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(3)
                        .balanceClientId(32)
                        .legalPartnerId(3)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosPartnersAnswer() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(2)
                        .balanceClientId(22)
                        .legalPartnerId(2)
                        .virtualAccountNumber("2")
                        .inn("2")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(3)
                        .balanceClientId(32)
                        .legalPartnerId(3)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getDtosPartnersAllIllegal() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(2)
                        .balanceClientId(22)
                        .legalPartnerId(2)
                        .virtualAccountNumber("2")
                        .inn("2")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(3)
                        .balanceClientId(32)
                        .legalPartnerId(3)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(4)
                        .balanceClientId(32)
                        .legalPartnerId(4)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(5)
                        .balanceClientId(32)
                        .legalPartnerId(5)
                        .virtualAccountNumber("3")
                        .inn("3")
                        .legalType("PVZ")
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getNewPartners() {
        return List.of(
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(10)
                        .balanceClientId(123)
                        .legalPartnerId(123)
                        .virtualAccountNumber("23")
                        .inn("2333")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(30)
                        .balanceClientId(320)
                        .legalPartnerId(30)
                        .virtualAccountNumber("30")
                        .inn("30")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(40)
                        .balanceClientId(320)
                        .legalPartnerId(40)
                        .virtualAccountNumber("30")
                        .inn("30")
                        .legalType("PVZ")
                        .build(),
                BillingLegalPartnerDto.builder()
                        .deliveryServiceId(50)
                        .balanceClientId(320)
                        .legalPartnerId(50)
                        .virtualAccountNumber("30")
                        .inn("30")
                        .legalType("PVZ")
                        .build()
        );
    }

    @Nonnull
    private List<BillingLegalPartnerDto> getAllLegalPartners() {
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
                        .build()
        );
    }
}
