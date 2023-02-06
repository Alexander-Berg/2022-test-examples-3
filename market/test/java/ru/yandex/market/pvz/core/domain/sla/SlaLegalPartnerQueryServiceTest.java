package ru.yandex.market.pvz.core.domain.sla;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.sla.entity.SlaLegalPartner;
import ru.yandex.market.pvz.core.domain.sla.mapper.SlaParamsMapper;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaLegalPartnerQueryServiceTest {

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final SlaLegalPartnerQueryService slaLegalPartnerQueryService;
    private final SlaLegalPartnerRepository slaLegalPartnerRepository;
    private final SlaParamsMapper mapper;

    @Test
    void getLegalPartnerSla() {
        var partner = legalPartnerFactory.createLegalPartner();
        var reportMonth = "2021-10";

        var defaultValue = 0.83;

        var expected = mapper.map(slaLegalPartnerRepository.save(SlaLegalPartner.builder()
                .legalPartner(partner)
                .reportMonth("2021-10")
                .arrivedOrdersCount(1000L)
                .acceptTimeliness(defaultValue)
                .storageTermTimeliness(defaultValue)
                .redemptionRate(defaultValue)
                .hasActualDebt(false)
                .hadDebtInActualMonth(true)
                .clientComplaint(defaultValue)
                .courierComplaint(defaultValue)
                .supplierComplaint(defaultValue)
                .totalComplaint(defaultValue)
                .rating(BigDecimal.valueOf(defaultValue))
                .modificationDate(LocalDate.now())
                .build()));

        var slaLegalPartner = slaLegalPartnerQueryService.getSlaForLegalPartner(partner.getId(), reportMonth);


        assertThat(slaLegalPartner).isEqualTo(expected);
    }
}
