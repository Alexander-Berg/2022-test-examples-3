package ru.yandex.market.pvz.core.test.factory;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationDetails;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.core.domain.legal_partner.termination.LegalPartnerTerminationCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.termination.LegalPartnerTerminationParams;

@Transactional
public class TestLegalPartnerTerminationFactory {

    @Autowired
    private LegalPartnerTerminationCommandService legalPartnerTerminationCommandService;

    public LegalPartnerTerminationParams createLegalPartnerTermination() {
        return createLegalPartnerTermination(LegalPartnerTestParamsBuilder.builder().build());
    }

    public LegalPartnerTerminationParams createLegalPartnerTermination(LegalPartnerTestParamsBuilder params) {
        return legalPartnerTerminationCommandService.create(
                buildLegalPartnerTerminationParams(params.getParams())
        );
    }

    public void cancelPartnerTermination(LegalPartnerTestParamsBuilder params) {
        legalPartnerTerminationCommandService.cancel(
                params.getParams().getLegalPartnerId(), params.getParams().getType());
    }

    private LegalPartnerTerminationParams buildLegalPartnerTerminationParams(
            LegalPartnerTerminationTestParams params
    ) {
        return LegalPartnerTerminationParams
                .builder()
                .active(params.isActive())
                .legalPartnerId(params.getLegalPartnerId())
                .fromTime(params.getFromTime())
                .toTime(params.getToTime())
                .type(params.getType())
                .build();
    }

    @Data
    @Builder
    public static class LegalPartnerTestParamsBuilder {

        @Builder.Default
        private LegalPartnerTerminationTestParams params = LegalPartnerTerminationTestParams.builder().build();

    }

    @Data
    @Builder
    public static class LegalPartnerTerminationTestParams {

        public static final OffsetDateTime DEFAULT_FROM_TIME = OffsetDateTime.now();
        public static final Boolean DEFAULT_ACTIVE = true;
        public static final LegalPartnerTerminationType DEFAULT_TYPE = LegalPartnerTerminationType.DEBT;

        @Builder.Default
        private OffsetDateTime fromTime = DEFAULT_FROM_TIME;

        @Builder.Default
        private boolean active = DEFAULT_ACTIVE;

        @Builder.Default
        private LegalPartnerTerminationType type = DEFAULT_TYPE;

        private long legalPartnerId;
        private OffsetDateTime toTime;
        private LegalPartnerTerminationDetails details;

    }

}
