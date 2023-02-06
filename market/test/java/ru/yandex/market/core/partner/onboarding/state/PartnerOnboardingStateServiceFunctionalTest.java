package ru.yandex.market.core.partner.onboarding.state;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DbUnitDataSet(before = "PartnerOnboardingStateServiceFunctionalTest.before.csv")
class PartnerOnboardingStateServiceFunctionalTest extends FunctionalTest {
    @Autowired
    private PartnerOnboardingStateService service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Instant now = Instant.now();

    @Test
    void saveStateParamsNew() {
        // given
        var partnerId = 1L;
        var namespace = PartnerOnboardingStateParamNamespace.UNKNOWN;
        var paramName = "APPLICATION_STATUS";
        var paramValue = "STATUS1";

        Optional<Instant> firstCalcTime = service.getStateLastCalculationTime(partnerId);

        // when
        service.savePartnerState(partnerId, now, List.of(
                new PartnerOnboardingStateRecord.Builder()
                        .withPartnerId(partnerId)
                        .withNamespace(namespace)
                        .withParamName(paramName)
                        .withParamValue(paramValue)
                        .build()
        ));
        var state = service.getOnboardingState(partnerId, namespace, paramName);

        Optional<Instant> secondCalcTime = service.getStateLastCalculationTime(partnerId);

        //then
        assertThat(state).hasValueSatisfying(v -> {
            assertThat(v.getPartnerId()).isEqualTo(partnerId);
            assertThat(v.getNamespace()).isEqualTo(namespace);
            assertThat(v.getParamName()).isEqualTo(paramName);
            assertThat(v.getParamValue()).isEqualTo(paramValue);
            assertThat(v.getUpdateTime()).isNotNull();
        });

        assertThat(firstCalcTime).isEmpty();
        assertThat(secondCalcTime).isPresent();
        assertThat(secondCalcTime.orElseThrow()).isEqualTo(now);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingStateFunctionalTest.saveStateParamsUpdate.before.csv")
    void saveStateParamsUpdate() {
        // given
        var partnerId = 1L;
        var namespace = PartnerOnboardingStateParamNamespace.COMMON;
        var paramName = "APPLICATION_STATUS";

        // when
        PartnerOnboardingStateRecord stateFirst = service.getOnboardingState(
                partnerId,
                namespace,
                paramName
        ).orElseThrow();

        service.savePartnerState(1L, now, List.of(
                new PartnerOnboardingStateRecord.Builder()
                        .withPartnerId(partnerId)
                        .withNamespace(namespace)
                        .withParamName(paramName)
                        .withParamValue("STATUS2")
                        .build()
        ));

        PartnerOnboardingStateRecord stateSecond = service.getOnboardingState(
                partnerId,
                namespace,
                paramName
        ).orElseThrow();

        Instant calcTime = service.getStateLastCalculationTime(partnerId).orElseThrow();

        // then
        assertThat(stateSecond.getUpdateTime()).isAfter(stateFirst.getUpdateTime());
        assertThat(calcTime).isEqualTo(now);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingStateFunctionalTest.saveStateParamsUpdate.before.csv")
    void saveStateParamsUpdateWithLock() {
        // given
        var partnerId = 1L;
        var namespace = PartnerOnboardingStateParamNamespace.COMMON;
        var paramName = "APPLICATION_STATUS";

        // when
        PartnerOnboardingStateRecord stateFirst = service.getOnboardingState(
                partnerId,
                namespace,
                paramName
        ).orElseThrow();

        transactionTemplate.execute(ignored -> {
            service.lockPartnerStateAndGetCalcTime(partnerId);
            service.savePartnerState(1L, now, List.of(
                    new PartnerOnboardingStateRecord.Builder()
                            .withPartnerId(partnerId)
                            .withNamespace(namespace)
                            .withParamName(paramName)
                            .withParamValue("STATUS2")
                            .build()
            ));

            return null;
        });

        PartnerOnboardingStateRecord stateSecond = service.getOnboardingState(
                partnerId,
                namespace,
                paramName
        ).orElseThrow();

        Instant calcTime = service.getStateLastCalculationTime(partnerId).orElseThrow();

        // then
        assertThat(stateSecond.getUpdateTime()).isAfter(stateFirst.getUpdateTime());
        assertThat(calcTime).isEqualTo(now);
    }

    @Test
    void testLockWithoutTransaction() {
        assertThatThrownBy(() -> {
            service.lockPartnerStateAndGetCalcTime(1L);
        }).isInstanceOf(NoTransactionException.class);
    }

    @Test
    void saveEmptyState() {
        //given
        var partnerId = 1L;

        //when
        Optional<Instant> calcTimeBefore = service.getStateLastCalculationTime(partnerId);
        service.savePartnerState(partnerId, now, List.of());
        Instant calcTime = service.getStateLastCalculationTime(1L).orElseThrow();

        //then
        assertThat(calcTimeBefore).isEmpty();
        assertThat(calcTime).isEqualTo(now);
    }

    @Test
    void testDifferentPartnerRecords() {
        //given
        var partnerId = 1L;
        var otherPartnerId = 2L;
        var namespace = PartnerOnboardingStateParamNamespace.COMMON;
        var paramName = "APPLICATION_STATUS";

        //when,then
        assertThatThrownBy(() -> {
            service.savePartnerState(partnerId, now, List.of(
                    new PartnerOnboardingStateRecord.Builder()
                            .withPartnerId(otherPartnerId)
                            .withNamespace(namespace)
                            .withParamName(paramName)
                            .withParamValue("STATUS2")
                            .build()
            ));
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingStateFunctionalTest.getOnboardingStates.before.csv")
    void getOnboardingStates() {
        // when
        var partnerStates = service.getOnboardingStates(1L);

        // then
        assertThat(partnerStates).containsExactlyInAnyOrder(
                new PartnerOnboardingStateRecord.Builder()
                        .withPartnerId(1L)
                        .withNamespace(PartnerOnboardingStateParamNamespace.COMMON)
                        .withParamName("PARAM1")
                        .withParamValue("VALUE1")
                        .withUpdateDate(Instant.parse("2020-10-15T02:00:00Z"))
                        .build(),
                new PartnerOnboardingStateRecord.Builder()
                        .withPartnerId(1L)
                        .withNamespace(PartnerOnboardingStateParamNamespace.WIZARD)
                        .withParamName("PARAM2")
                        .withParamValue("VALUE2")
                        .withUpdateDate(Instant.parse("2020-10-15T03:00:00Z"))
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingStateFunctionalTest.getOnboardingStates.before.csv")
    void getOnboardingStatesForNameSpace() {
        // when
        var partnerStates = service.getOnboardingStates(1L, PartnerOnboardingStateParamNamespace.WIZARD);

        // then
        assertThat(partnerStates).containsExactlyInAnyOrder(
                new PartnerOnboardingStateRecord.Builder()
                        .withPartnerId(1L)
                        .withNamespace(PartnerOnboardingStateParamNamespace.WIZARD)
                        .withParamName("PARAM2")
                        .withParamValue("VALUE2")
                        .withUpdateDate(Instant.parse("2020-10-15T03:00:00Z"))
                        .build()
        );
    }
}
