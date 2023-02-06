package ru.yandex.market.core.cutoff.listener;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgram;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "CpcPartnerPlacementEventListenerTest.before.csv")
class CpcPartnerPlacementEventListenerTest extends FunctionalTest {
    private static final Long PARTNER_ID = 1001L;
    private static final Long TESTED_PARTNER_ID = 1002L;
    @Autowired
    CutoffService cutoffService;
    @Autowired
    PartnerPlacementProgramService partnerPlacementProgramService;
    @Autowired
    TransactionTemplate transactionTemplate;

    static Stream<Arguments> getCutoffsArguments() {
        return Stream.of(
                //SUCCESS
                Arguments.of(PARTNER_ID,
                        List.of(CutoffType.FINANCE),
                        List.of(CutoffType.FINANCE), PartnerPlacementProgramStatus.SUCCESS, true),
                //FAIL
                Arguments.of(PARTNER_ID,
                        List.of(CutoffType.QMANAGER_CLONE),
                        List.of(), PartnerPlacementProgramStatus.FAIL, false),
                //CONFIGURE
                Arguments.of(PARTNER_ID,
                        List.of(CutoffType.FINANCE, CutoffType.FORTESTING, CutoffType.TECHNICAL_NEED_INFO),
                        List.of(), PartnerPlacementProgramStatus.CONFIGURE, false),
                //CONFIGURE
                Arguments.of(PARTNER_ID,
                        List.of(CutoffType.FINANCE, CutoffType.FORTESTING, CutoffType.TECHNICAL_NEED_INFO),
                        List.of(), PartnerPlacementProgramStatus.CONFIGURE, false),
                //TESTED
                Arguments.of(TESTED_PARTNER_ID,
                        List.of(CutoffType.FINANCE, CutoffType.FORTESTING), List.of(),
                        PartnerPlacementProgramStatus.TESTED, false),
                //TESTED FAILED
                Arguments.of(1003L,
                        List.of(CutoffType.FINANCE, CutoffType.FORTESTING), List.of(),
                        PartnerPlacementProgramStatus.CONFIGURE, false)

        );
    }

    @ParameterizedTest(name = "partnerId: {0},expectedStatus={3}, openCutoffs: {1}, closedCutoffs: {2}")
    @MethodSource("getCutoffsArguments")
    void testCutoff(
            long partnerId,
            List<CutoffType> openCutoffs,
            List<CutoffType> closedCutoffs,
            PartnerPlacementProgramStatus expectedStatus,
            boolean isEverActivated
    ) {
        transactionTemplate.execute(status -> {
            status.setRollbackOnly();

            openCutoffs.forEach(cutoff -> cutoffService.openCutoff(partnerId, cutoff, 1L));
            closedCutoffs.forEach(cutoff -> cutoffService.closeCutoff(partnerId, cutoff, 1L));
            var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(partnerId);
            assertThat(programs.size()).isEqualTo(1L);
            PartnerPlacementProgram actual = programs.get(PartnerPlacementProgramType.CPC);
            assertThat(actual).as("CPC program not found: " + programs.keySet()).isNotNull();

            assertThat(actual.getPartnerId()).isEqualTo(partnerId);
            assertThat(actual.getProgram()).isEqualTo(PartnerPlacementProgramType.CPC);
            assertThat(actual.getStatus()).isEqualTo(expectedStatus);
            assertThat(actual.isEverActivated()).isEqualTo(isEverActivated);
            return null;
        });
    }
}
