package ru.yandex.market.api.billing;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartnerContractOptionsServiceFunctionalTest extends FunctionalTest {

    @Autowired
    private MbiBillingClient mbiBillingClient;

    @Autowired
    private PartnerContractOptionsService partnerContractOptionsService;

    @Test
    @DbUnitDataSet(before = "PartnerContractOptionsServiceFunctionalTest.before.csv")
    @DisplayName("Возврат контрактов на бизнесе данного партнера")
    public void getPayoutFrequencyInfoOnBusinessTest() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(1000, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.WEEKLY, false),
                        createFrequencyDTO(2000, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.DAILY, true)
                ));

        long partnerId = 1L;
        PartnerContractOptionsWithFrequency options =
                partnerContractOptionsService.getContractOptionsForPartner(partnerId);

        assertThat(options.getCurrentContractId()).hasValue(1000L);

        List<PartnerContractOptionWithFrequency> optionList = options.getPossibleContracts();
        assertThat(optionList).hasSize(2);

        PartnerContractOptionWithFrequency info1 = optionList.get(0);
        assertThat(info1.getContractId()).isEqualTo(1000);
        assertThat(info1.getJurName()).isEqualTo("Организация 1");
        assertThat(info1.getCurrentMonthFrequency()).isEqualTo(PayoutFrequency.DAILY);
        assertThat(info1.getNextMonthFrequency()).isEqualTo(PayoutFrequency.WEEKLY);
        assertThat(info1.isDefaultCurrentMonthFrequency()).isFalse();
        assertThat(info1.getContractEid()).isEqualTo("100");

        PartnerContractOptionWithFrequency info2 = optionList.get(1);
        assertThat(info2.getContractId()).isEqualTo(2000);
        assertThat(info2.getJurName()).isEqualTo("Организация 2");
        assertThat(info2.getCurrentMonthFrequency()).isEqualTo(PayoutFrequency.WEEKLY);
        assertThat(info2.getNextMonthFrequency()).isEqualTo(PayoutFrequency.DAILY);
        assertThat(info2.isDefaultCurrentMonthFrequency()).isTrue();
        assertThat(info2.getContractEid()).isEqualTo("200");
    }

    @Test
    @DbUnitDataSet(before = "PartnerContractOptionsServiceFunctionalTest.before.csv")
    @DisplayName("Возврат контрактов на бизнесе данного партнера - для части контрактов нет выплаты")
    public void getParticularPaymentFrequencyOnPartnerContracts() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(2000, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.DAILY, false)
                ));

        long partnerId = 1L;

        PartnerContractOptionsWithFrequency options =
                partnerContractOptionsService.getContractOptionsForPartner(partnerId);

        assertThat(options.getCurrentContractId()).hasValue(1000L);

        List<PartnerContractOptionWithFrequency> optionList = options.getPossibleContracts();

        PartnerContractOptionWithFrequency info1 = optionList.get(0);
        assertThat(info1.getContractId()).isEqualTo(1000);
        assertThat(info1.getJurName()).isEqualTo("Организация 1");
        assertThat(info1.getCurrentMonthFrequency()).isNull();
        assertThat(info1.getNextMonthFrequency()).isNull();
        assertThat(info1.getContractEid()).isEqualTo("100");

        PartnerContractOptionWithFrequency info2 = optionList.get(1);
        assertThat(info2.getContractId()).isEqualTo(2000);
        assertThat(info2.getJurName()).isEqualTo("Организация 2");
        assertThat(info2.getCurrentMonthFrequency()).isEqualTo(PayoutFrequency.WEEKLY);
        assertThat(info2.getNextMonthFrequency()).isEqualTo(PayoutFrequency.DAILY);
        assertThat(info2.getContractEid()).isEqualTo("200");
    }

    @Test
    @DbUnitDataSet(before = "PartnerContractOptionsServiceFunctionalTest.before.csv")
    @DisplayName("Для партнера без контрактов мы не ходим в биллинг вообще")
    public void getOptionsForPartnerWithoutContracts() {
        long partnerId = 4L;
        PartnerContractOptionsWithFrequency options =
                partnerContractOptionsService.getContractOptionsForPartner(partnerId);

        assertThat(options.getCurrentContractId()).isEmpty();
        assertThat(options.getPossibleContracts()).isEmpty();
        assertThat(options.isPaymentFrequencySetForCurrentContract()).isFalse();

        //Нет контрактов - не нужно дергать биллинг
        verify(mbiBillingClient, never()).getCurrentAndNextMonthPayoutFrequencies(any());
    }

    @Test
    @DbUnitDataSet(before = "PartnerContractOptionsServiceFunctionalTest.before.csv")
    @DisplayName("Возврат контрактов без частот выплат, контракты есть")
    public void getOptionsWithoutFrequencies() {
        long partnerId = 1L;
        PartnerContractOptions options = partnerContractOptionsService.getContractOptionsWithoutFrequencies(partnerId);

        assertThat(options.getCurrentContractId()).hasValue(1000L);

        List<PartnerContractOption> optionList = options.getPossibleContracts();
        assertThat(optionList).hasSize(2);

        PartnerContractOption info1 = optionList.get(0);
        assertThat(info1.getContractId()).isEqualTo(1000);
        assertThat(info1.getJurName()).isEqualTo("Организация 1");
        assertThat(info1.getContractEid()).isEqualTo("100");

        PartnerContractOption info2 = optionList.get(1);
        assertThat(info2.getContractId()).isEqualTo(2000);
        assertThat(info2.getJurName()).isEqualTo("Организация 2");
        assertThat(info2.getContractEid()).isEqualTo("200");

    }

    @Test
    @DbUnitDataSet(before = "PartnerContractOptionsServiceFunctionalTest.before.csv")
    @DisplayName("Возврат контрактов без частот выплат, контрактов нет")
    public void getOptionsWithoutFrequenciesEmptyAnswer() {
        long partnerId = 4L;
        PartnerContractOptions options = partnerContractOptionsService.getContractOptionsWithoutFrequencies(partnerId);

        assertThat(options.getCurrentContractId()).isEmpty();
        assertThat(options.getPossibleContracts()).isEmpty();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerContractOptionsServiceFunctionalTest.getPayoutFrequencyInfoOnBusinessDeleted.before.csv")
    @DisplayName("Возврат контрактов на бизнесе данного партнера, есть удаленный партнер")
    public void getPayoutFrequencyInfoOnBusinessDeleted() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(1000, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.WEEKLY, false)
                ));

        long partnerId = 1L;
        PartnerContractOptionsWithFrequency options =
                partnerContractOptionsService.getContractOptionsForPartner(partnerId);

        assertThat(options.getCurrentContractId()).hasValue(1000L);

        List<PartnerContractOptionWithFrequency> optionList = options.getPossibleContracts();
        assertThat(optionList).hasSize(1);

        PartnerContractOptionWithFrequency info1 = optionList.get(0);
        assertThat(info1.getContractId()).isEqualTo(1000);
        assertThat(info1.getJurName()).isEqualTo("Организация 1");
        assertThat(info1.getCurrentMonthFrequency()).isEqualTo(PayoutFrequency.DAILY);
        assertThat(info1.getNextMonthFrequency()).isEqualTo(PayoutFrequency.WEEKLY);
        assertThat(info1.isDefaultCurrentMonthFrequency()).isFalse();
        assertThat(info1.getContractEid()).isEqualTo("100");
    }

    private CurrentAndNextMonthPayoutFrequencyDTO createFrequencyDTO(
            long contractId,
            PayoutFrequencyDTO currentFrequency,
            PayoutFrequencyDTO nextFrequency,
            boolean isDefaultCurrentFrequency
    ) {
        CurrentAndNextMonthPayoutFrequencyDTO dto = new CurrentAndNextMonthPayoutFrequencyDTO();
        dto.setContractId(contractId);
        dto.setCurrentMonthFrequency(currentFrequency);
        dto.setNextMonthFrequency(nextFrequency);
        dto.isDefaultCurrentMonthFrequency(isDefaultCurrentFrequency);

        return dto;
    }
}
