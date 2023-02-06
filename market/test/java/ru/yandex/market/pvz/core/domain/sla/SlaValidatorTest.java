package ru.yandex.market.pvz.core.domain.sla;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.sla.yt.SlaLegalPartnerYtModel;
import ru.yandex.market.pvz.core.domain.sla.yt.SlaOrderYtModel;
import ru.yandex.market.pvz.core.domain.sla.yt.SlaPickupPointYtModel;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;


@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaValidatorTest {

    private static final String INCORRECT_MODIFICATION_DATE_1 = "1970-01-01";
    private static final String INCORRECT_MODIFICATION_DATE_2 = "01-01-2022";
    private static final String CORRECT_MODIFICATION_DATE = "2022-01-01";
    private static final String REPORT_MONTH = "2022-01";

    private final SlaValidator slaValidator;

    @Test
    void validateIncorrectLegalPartners() {

        var validatedSlas = slaValidator.validateSlaLegalPartners(List.of(
                createIncorrectSlaLegalPartner(INCORRECT_MODIFICATION_DATE_1),
                createIncorrectSlaLegalPartner(INCORRECT_MODIFICATION_DATE_2),
                createIncorrectSlaLegalPartner(CORRECT_MODIFICATION_DATE)));

        assertThat(validatedSlas.size()).isEqualTo(1);
        assertThat(validatedSlas.get(0)).isEqualTo(createEmptySlaLegalPartner());
    }

    @Test
    void validateIncorrectPickupPoints() {
        var validatedSlas = slaValidator.validateSlaPickupPoints(List.of(
                createIncorrectSlaPickupPoint(INCORRECT_MODIFICATION_DATE_1),
                createIncorrectSlaPickupPoint(INCORRECT_MODIFICATION_DATE_2),
                createIncorrectSlaPickupPoint(CORRECT_MODIFICATION_DATE)));

        assertThat(validatedSlas.size()).isEqualTo(1);
        assertThat(validatedSlas.get(0)).isEqualTo(createEmptySlaPickupPoint());
    }

    @Test
    void validateIncorrectOrders() {
        var validatedSlas = slaValidator.validateSlaOrders(List.of(
                createIncorrectSlaOrder(INCORRECT_MODIFICATION_DATE_1),
                createIncorrectSlaOrder(INCORRECT_MODIFICATION_DATE_2),
                createIncorrectSlaOrder(CORRECT_MODIFICATION_DATE)));

        assertThat(validatedSlas.size()).isEqualTo(1);
        assertThat(validatedSlas.get(0)).isEqualTo(createEmptySlaOrder());
    }

    private SlaLegalPartnerYtModel createIncorrectSlaLegalPartner(String modificationDate) {
        return SlaLegalPartnerYtModel.builder()
                .modificationDate(modificationDate)
                .reportMonth(REPORT_MONTH)
                .arrivedOrdersCount(-1L)
                .acceptTimeliness(200.0)
                .hasActualDebt(3)
                .rating(BigDecimal.valueOf(-50.0))
                .build();
    }

    private SlaLegalPartnerYtModel createEmptySlaLegalPartner() {
        return SlaLegalPartnerYtModel.builder()
                .modificationDate(CORRECT_MODIFICATION_DATE)
                .reportMonth(REPORT_MONTH)
                .build();
    }

    private SlaPickupPointYtModel createIncorrectSlaPickupPoint(String modificationDate) {
        return SlaPickupPointYtModel.builder()
                .modificationDate(modificationDate)
                .reportMonth(REPORT_MONTH)
                .arrivedOrdersCount(-1L)
                .acceptTimeliness(200.0)
                .hasActualDebt(3)
                .rating(-50L)
                .build();
    }

    private SlaPickupPointYtModel createEmptySlaPickupPoint() {
        return SlaPickupPointYtModel.builder()
                .modificationDate(CORRECT_MODIFICATION_DATE)
                .reportMonth(REPORT_MONTH)
                .build();
    }

    private SlaOrderYtModel createIncorrectSlaOrder(String modificationDate) {
        return SlaOrderYtModel.builder()
                .modificationDate(modificationDate)
                .finalStatusDate(modificationDate)
                .clientComplaint(4)
                .deliveredToRecipientDate(INCORRECT_MODIFICATION_DATE_2)
                .build();
    }

    private SlaOrderYtModel createEmptySlaOrder() {
        return SlaOrderYtModel.builder()
                .modificationDate(CORRECT_MODIFICATION_DATE)
                .finalStatusDate(CORRECT_MODIFICATION_DATE)
                .build();
    }
}
