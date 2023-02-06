package ru.yandex.market.pvz.internal.domain.lms.legal_partner;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.legal_partner.termination.LegalPartnerTerminationParams;
import ru.yandex.market.pvz.core.domain.legal_partner.termination.LegalPartnerTerminationQueryService;
import ru.yandex.market.pvz.core.domain.legal_partner.termination.LegalPartnerTerminationRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.internal.domain.lms.legal_partner.dto.LmsPartnerBatchToggleDto;
import ru.yandex.market.pvz.internal.domain.lms.legal_partner.model.LegalPartnerCancelDeactivationModel;
import ru.yandex.market.pvz.internal.domain.lms.legal_partner.model.LegalPartnerDeactivationModel;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Import(LmsBatchPartnerToggleService.class)
class LmsBatchPartnerToggleServiceTest {

    private static final OffsetDateTime FROM_TIME = OffsetDateTime.of(2022, 1, 25, 11, 0, 0, 0,
            DateTimeUtil.DEFAULT_ZONE_ID);
    private static final String FROM_TIME_STRING = FROM_TIME.format(LmsBatchPartnerToggleService.DATE_TIME_FORMATTER);

    private final LmsBatchPartnerToggleService lmsBatchPartnerToggleService;

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final LegalPartnerQueryService legalPartnerQueryService;

    private final TestLegalPartnerTerminationFactory terminationFactory;
    private final LegalPartnerTerminationQueryService terminationQueryService;
    private final LegalPartnerTerminationRepository terminationRepository;

    private LegalPartner legalPartner;

    @BeforeEach
    void init() {
        legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory
                .forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));
    }

    @Test
    void testDeactivateSuccess() {
        lmsBatchPartnerToggleService.batchPartnerToggle(new LmsPartnerBatchToggleDto(
                buildDeactivationCsv(List.of(LegalPartnerDeactivationModel.builder()
                        .legalPartnerId(legalPartner.getId())
                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                        .fromTime(FROM_TIME_STRING)
                        .build())),
                null
        ));

        var terminationParamsDb = terminationQueryService.findActiveTerminationByLegalPartnerId(legalPartner.getId())
                .get(0);

        LegalPartnerTerminationParams expected = LegalPartnerTerminationParams.builder()
                .id(terminationParamsDb.getId())
                .legalPartnerId(legalPartner.getId())
                .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                .fromTime(FROM_TIME)
                .active(true)
                .build();

        assertThat(terminationParamsDb).isEqualToIgnoringNullFields(expected);
    }

    @Test
    void testDeactivateError() {
        ResponseEntity<?> responseEntity = lmsBatchPartnerToggleService.batchPartnerToggle(new LmsPartnerBatchToggleDto(
                buildDeactivationCsv(List.of(LegalPartnerDeactivationModel.builder()
                        .legalPartnerId(legalPartner.getId() + 1000)
                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                        .fromTime(FROM_TIME_STRING)
                        .build())),
                null
        ));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testReactivateSuccess() {
        lmsBatchPartnerToggleService.batchPartnerToggle(new LmsPartnerBatchToggleDto(
                buildDeactivationCsv(List.of(LegalPartnerDeactivationModel.builder()
                        .legalPartnerId(legalPartner.getId())
                        .type(LegalPartnerTerminationType.DEBT)
                        .fromTime(FROM_TIME_STRING)
                        .build())),
                null
        ));

        lmsBatchPartnerToggleService.batchPartnerToggle(new LmsPartnerBatchToggleDto(
                null,
                buildActivationCsv(List.of(LegalPartnerCancelDeactivationModel.builder()
                        .legalPartnerId(legalPartner.getId())
                        .type(LegalPartnerTerminationType.DEBT)
                        .build()))
        ));

        assertThat(terminationQueryService.findActiveTerminationByLegalPartnerId(legalPartner.getId())).isEmpty();
    }


    @Test
    void testReactivateError() {
        ResponseEntity<?> responseEntity = lmsBatchPartnerToggleService.batchPartnerToggle(new LmsPartnerBatchToggleDto(
                null,
                buildActivationCsv(List.of(LegalPartnerCancelDeactivationModel.builder()
                        .legalPartnerId(legalPartner.getId() + 1000)
                        .type(LegalPartnerTerminationType.DEBT)
                        .build()))
        ));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private byte[] buildDeactivationCsv(List<LegalPartnerDeactivationModel> deactivations) {
        StringBuilder csvBuilder = new StringBuilder("legalPartnerId,type,fromTime\n");
        for (LegalPartnerDeactivationModel deactivation : deactivations) {
            csvBuilder.append(sf(
                    "{},{},{}\n",
                    deactivation.getLegalPartnerId(),
                    deactivation.getType(),
                    deactivation.getFromTime()
            ));
        }
        return csvBuilder.toString().getBytes();
    }

    private byte[] buildActivationCsv(List<LegalPartnerCancelDeactivationModel> activations) {
        StringBuilder csvBuilder = new StringBuilder("legalPartnerId,type\n");
        for (LegalPartnerCancelDeactivationModel activation : activations) {
            csvBuilder.append(sf(
                    "{},{}\n",
                    activation.getLegalPartnerId(),
                    activation.getType()
            ));
        }
        return csvBuilder.toString().getBytes();
    }

}
