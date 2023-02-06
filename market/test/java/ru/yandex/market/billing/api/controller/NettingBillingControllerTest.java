package ru.yandex.market.billing.api.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.api.FunctionalTest;
import ru.yandex.market.billing.api.NettingBillingApi;
import ru.yandex.market.billing.api.model.BonusDTO;
import ru.yandex.market.billing.api.model.BonusTypeDTO;
import ru.yandex.market.billing.api.model.CurrencyDTO;
import ru.yandex.market.billing.api.model.NettingBillingInfoDTO;
import ru.yandex.market.billing.api.model.NettingTransitionInfoDTO;
import ru.yandex.market.billing.core.cpa_auction.CpaAuctionBillingDao;
import ru.yandex.market.billing.core.netting.model.NettingTransitionStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class NettingBillingControllerTest extends FunctionalTest {
    @Autowired
    CpaAuctionBillingDao billingDao;
    @Autowired
    NettingBillingApi controller;

    @Autowired
    TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-07-13T00:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DbUnitDataSet(before = "NettingBillingControllerTest.before.csv")
    void testGetBillingInfo() {
        var expected1 = new NettingBillingInfoDTO()
                .currency(CurrencyDTO.RUR)
                .bonusesInfo(List.of(new BonusDTO().partnerId(1L).bonusSum(10000.00)
                        .expiredAt(LocalDate.of(2021, 8, 6))
                        .bonusType(BonusTypeDTO.NEWBIE)))
                .spentAmount(0L);
        var expected910 = new NettingBillingInfoDTO()
                .currency(CurrencyDTO.RUR)
                .bonusesInfo(List.of())
                .spentAmount(0L);
        ResponseEntity<NettingBillingInfoDTO> result1 = controller.getBillingInfo(1L);
        ResponseEntity<NettingBillingInfoDTO> result9 = controller.getBillingInfo(9L);
        ResponseEntity<NettingBillingInfoDTO> result10 = controller.getBillingInfo(10L);
        Assertions.assertThat(result1.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(result1.getBody()).isEqualTo(expected1);
        Assertions.assertThat(result9.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(result9.getBody()).isEqualTo(expected910);
        Assertions.assertThat(result10.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(result10.getBody()).isEqualTo(expected910);
    }

    @Test
    @DbUnitDataSet(after = "NettingTransition.save.after.csv")
    void testSaveNettingTransition() {
        var nettingTransitions = List.of(
                buildDTO(1L, NettingTransitionStatus.ENABLED),
                buildDTO(2L, NettingTransitionStatus.DISABLED),
                buildDTO(3L, NettingTransitionStatus.REJECTED)
        );

        var result = controller.saveNettingTransition(nettingTransitions);

        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DbUnitDataSet(
            before = "NettingTransition.idem.before.csv",
            after = "NettingTransition.idem.after.csv"
    )
    void testSaveNettingTransitionIdempotency() {
        var nettingTransitions = List.of(
                buildDTO(1L, NettingTransitionStatus.ENABLED),
                buildDTO(2L, NettingTransitionStatus.ENABLED),
                buildDTO(3L, NettingTransitionStatus.ENABLED)
        );

        var result = controller.saveNettingTransition(nettingTransitions);

        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    private NettingTransitionInfoDTO buildDTO(long partnerId, NettingTransitionStatus status) {
        NettingTransitionInfoDTO nettingTransitionInfoDTO = new NettingTransitionInfoDTO();
        nettingTransitionInfoDTO.setPartnerId(partnerId);
        nettingTransitionInfoDTO.setStatus(
                ru.yandex.market.billing.api.model.NettingTransitionStatus.valueOf(status.name())
        );
        return nettingTransitionInfoDTO;
    }
}
