package ru.yandex.market.pvz.core.domain.legal_partner.indebtedness;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerIndebtednessCommandServiceTest {

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final LegalPartnerIndebtednessCommandService legalPartnerIndebtednessCommandService;
    private final LegalPartnerIndebtednessRepository legalPartnerIndebtednessRepository;

    @Test
    void updateIndebtednessTest() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        BigDecimal debtSum = new BigDecimal(44000);
        var legalPartnerIndebtednessParams = LegalPartnerIndebtednessParams.builder()
                .legalPartnerId(legalPartner.getId())
                .debtSum(debtSum)
                .build();

        var result = legalPartnerIndebtednessCommandService.updateIndebtedness(legalPartnerIndebtednessParams);

        assertThat(result.getLegalPartnerId()).isEqualTo(legalPartner.getId());
        assertThat(result.getDebtDate()).isEqualTo(LocalDate.now());
        assertThat(result.getDebtSum()).isEqualTo(debtSum);
    }
}
