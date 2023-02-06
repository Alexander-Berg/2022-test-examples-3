package ru.yandex.market.pvz.core.domain.sla;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.sla.entity.SlaLegalPartner;
import ru.yandex.market.pvz.core.domain.sla.yt.SlaLegalPartnerYtModel;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;


@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaLegalPartnerCommandServiceTest {

    private static final String DEFAULT_REPORT_MONTH = "2022-02";

    private final SlaLegalPartnerCommandService slaLegalPartnerCommandService;
    private final SlaLegalPartnerRepository slaLegalPartnerRepository;
    private final TestLegalPartnerFactory legalPartnerFactory;

    @Test
    void saveAndUpdateSlaOrderRows() {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        var model = createYtModel(legalPartner.getId(), 0);
        slaLegalPartnerCommandService.saveOrUpdateSlaLegalPartnerRows(Collections.singletonList(model));

        var slaLegalPartners = slaLegalPartnerRepository.findAll();
        assertThat(slaLegalPartners.size()).isEqualTo(1);
        assertThat(slaLegalPartners.get(0)).isEqualToIgnoringGivenFields(createSlaLegalPartner(model), "id",
                "createdAt", "updatedAt", "legalPartner");

        model = createYtModel(legalPartner.getId(), 1);
        slaLegalPartnerCommandService.saveOrUpdateSlaLegalPartnerRows(Collections.singletonList(model));

        slaLegalPartners = slaLegalPartnerRepository.findAll();
        assertThat(slaLegalPartners.size()).isEqualTo(1);
        assertThat(slaLegalPartners.get(0)).isEqualToIgnoringGivenFields(createSlaLegalPartner(model), "id",
                "createdAt", "updatedAt", "legalPartner");

    }

    @Test
    void saveSlaOrderRowsWithNoExternalId() {
        var model = createYtModel(null, 0);
        slaLegalPartnerCommandService.saveOrUpdateSlaLegalPartnerRows(Collections.singletonList(model));

        var slaPickupPoints = slaLegalPartnerRepository.findAll();
        assertThat(slaPickupPoints.size()).isEqualTo(0);
    }

    private SlaLegalPartnerYtModel createYtModel(Long legalPartnerId, double acceptTimelines) {
        return SlaLegalPartnerYtModel.builder()
                .legalPartnerId(legalPartnerId)
                .acceptTimeliness(acceptTimelines)
                .reportMonth(DEFAULT_REPORT_MONTH)
                .build();
    }

    private SlaLegalPartner createSlaLegalPartner(SlaLegalPartnerYtModel model) {
        return SlaLegalPartner.builder()
                .acceptTimeliness(model.getAcceptTimeliness())
                .reportMonth(DEFAULT_REPORT_MONTH)
                .build();
    }
}
