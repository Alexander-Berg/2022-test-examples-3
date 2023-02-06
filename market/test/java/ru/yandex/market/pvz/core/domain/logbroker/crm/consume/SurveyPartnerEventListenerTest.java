package ru.yandex.market.pvz.core.domain.logbroker.crm.consume;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadDto;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadType;
import ru.yandex.market.pvz.client.crm.dto.PartnerSurveyCrmDto;
import ru.yandex.market.pvz.client.model.survey.PartnerSurveyStatus;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.survey.SurveyParams;
import ru.yandex.market.pvz.core.domain.survey.SurveyPartnerCommandService;
import ru.yandex.market.pvz.core.domain.survey.SurveyPartnerParams;
import ru.yandex.market.pvz.core.domain.survey.SurveyPartnerQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestSurveyFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.client.model.survey.PartnerSurveyStatus.PROCESSING;
import static ru.yandex.market.pvz.client.model.survey.PartnerSurveyStatus.REGISTERED;
import static ru.yandex.market.pvz.core.domain.survey.SurveyPartnerCommandService.OW_ID_PREFIX;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SurveyPartnerEventListenerTest {

    private static final long OW_ID = 1234;
    private static final long WRONG_OW_ID = 12345;
    private static final PartnerSurveyStatus FIRST_OW_STATUS = REGISTERED;
    private static final PartnerSurveyStatus SECOND_OW_STATUS = PROCESSING;

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestSurveyFactory surveyFactory;
    private final TestableClock clock;
    private final SurveyPartnerEventListener eventListener;
    private final SurveyPartnerQueryService surveyPartnerQueryService;
    private final SurveyPartnerCommandService surveyPartnerCommandService;

    @Test
    void whenHandleThenSuccess() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        createSurveyPartner(legalPartner);
        var crmPayloadDto = buildCrmPayload(legalPartner, OW_ID);
        eventListener.handle(crmPayloadDto);
        var surveyPartner = surveyPartnerQueryService.findByLegalPartnerIdAndOwId(legalPartner.getId(), OW_ID);
        assertThat(surveyPartner.getOwStatus()).isEqualTo(SECOND_OW_STATUS);
    }

    @Test
    void whenHandleThenError() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        createSurveyPartner(legalPartner);
        var crmPayloadDto = buildCrmPayload(legalPartner, WRONG_OW_ID);
        assertThatThrownBy(() -> eventListener.handle(crmPayloadDto))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    private void createSurveyPartner(LegalPartner legalPartner) {
        SurveyParams survey = surveyFactory.create();
        surveyPartnerCommandService.create(SurveyPartnerParams.builder()
                .surveyId(survey.getId())
                .legalPartnerId(legalPartner.getId())
                .showDateTime(clock.instant())
                .owId(OW_ID)
                .owStatus(FIRST_OW_STATUS)
                .build());
    }

    private CrmPayloadDto buildCrmPayload(LegalPartner legalPartner, long owId) {
        return CrmPayloadDto.builder()
                .type(CrmPayloadType.PARTNER_SURVEY)
                .eventDateTime(clock.instant())
                .value(
                        PartnerSurveyCrmDto.builder()
                                .partnerId(legalPartner.getPartnerId())
                                .ticketGid(OW_ID_PREFIX + owId)
                                .status(SECOND_OW_STATUS)
                                .build()
                )
                .build();
    }
}
