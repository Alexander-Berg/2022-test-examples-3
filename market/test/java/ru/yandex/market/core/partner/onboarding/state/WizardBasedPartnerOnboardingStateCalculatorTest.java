package ru.yandex.market.core.partner.onboarding.state;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.onboarding.finder.PartnerBeingOnboarded;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.WizardService;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.MOSCOW_ID;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.RUSSIA_ID;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.dropshipInfo;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.notDropshipInfo;

class WizardBasedPartnerOnboardingStateCalculatorTest {
    PartnerOnboardingStateService partnerOnboardingStateService = mock(PartnerOnboardingStateService.class);
    WizardService wizardService = mock(WizardService.class);
    PartnerTypeAwareService partnerTypeAwareService = mock(PartnerTypeAwareService.class);
    ParamService paramService = mockParamService();
    PartnerOnboardingEventPublisher partnerOnboardingEventPublisher = mock(PartnerOnboardingEventPublisher.class);
    PartnerPlacementProgramService programService = mock(PartnerPlacementProgramService.class);

    WizardBasedPartnerOnboardingStateCalculator calculator = new WizardBasedPartnerOnboardingStateCalculator(
            partnerOnboardingStateService,
            partnerTypeAwareService,
            wizardService,
            paramService,
            partnerOnboardingEventPublisher,
            programService
    );

    static final long partnerIdLong = 1L;
    static final long campaignId = 10L;
    static final String partnerName = "partner";
    static final PartnerId partnerId = PartnerId.supplierId(partnerIdLong);
    static final Instant partnerCreatedAt = Instant.ofEpochSecond(12345L);
    static final Instant now = Instant.now();

    static final PartnerBeingOnboarded testCandidate = new PartnerBeingOnboarded(
            partnerId,
            campaignId,
            partnerName,
            partnerCreatedAt
    );


    @Test
    void calculatePartnerOnboardingStateCacheEmpty() {
        // given
        given(partnerOnboardingStateService.getOnboardingStates(eq(partnerId.toLong()), any()))
                .willReturn(List.of(
                        new PartnerOnboardingStateRecord.Builder()
                                .withPartnerId(partnerIdLong)
                                .withNamespace(PartnerOnboardingStateParamNamespace.COMMON)
                                .withParamName("someUnrelevantParameter")
                                .withParamValue("whatever")
                                .withUpdateDate(now)
                                .build()
                ));
        given(wizardService.getStatuses(eq(partnerId), any(), eq(true)))
                .willReturn(List.of(
                        makeWizardStepStatus(WizardStepType.SUPPLIER_INFO, Status.FULL),
                        makeWizardStepStatus(WizardStepType.ASSORTMENT, Status.EMPTY)
                ));
        given(partnerTypeAwareService.getPartnerTypeAwareInfo(partnerIdLong)).willReturn(dropshipInfo());

        // when
        var result = calculator.calculatePartnerOnboardingState(
                testCandidate,
                Suppliers.ofInstance(now)
        );

        // then
        assertThat(result.getPartnerId())
                .isSameAs(partnerId);
        assertThat(result.isDropship()).isTrue();
        assertThat(result.getCountryId()).isEqualTo(RUSSIA_ID);
        assertThat(result.getLocalRegionId()).isEqualTo(MOSCOW_ID);
        assertThat(result.getPartnerCreatedAt())
                .isSameAs(partnerCreatedAt);
        assertThat(result.getStepDataList())
                .as("same statuses if no cache")
                .containsExactly(
                        makeStateStepStatus(WizardStepType.SUPPLIER_INFO, Status.FULL),
                        makeStateStepStatus(WizardStepType.ASSORTMENT, Status.EMPTY)
                );
        then(wizardService)
                .should(description("all steps should be calculated once"))
                .getStatuses(partnerId, EnumSet.allOf(WizardStepType.class), true);
        then(partnerOnboardingStateService)
                .should()
                .savePartnerState(partnerIdLong, now, List.of(
                        makeOnboardingMeta("SUPPLIER_INFO", "FULL"),
                        makeOnboardingMeta("ASSORTMENT", "EMPTY")
                ));
    }

    @Test
    void calculatePartnerOnboardingStateCachePartial() {
        // given
        given(partnerOnboardingStateService.getOnboardingStates(eq(partnerId.toLong()), any()))
                .willReturn(List.of(
                        makeOnboardingMeta("SUPPLIER_INFO", "FULL", now.minusSeconds(10L)),
                        makeOnboardingMeta("ASSORTMENT", "EMPTY")
                ));
        given(wizardService.getStatuses(eq(partnerId), any(), eq(true)))
                .willReturn(List.of(
                        makeWizardStepStatus(WizardStepType.ASSORTMENT, Status.FILLED)
                ));
        given(partnerTypeAwareService.getPartnerTypeAwareInfo(partnerIdLong)).willReturn(notDropshipInfo());

        // when
        var result = calculator.calculatePartnerOnboardingState(
                testCandidate,
                Suppliers.ofInstance(now)
        );

        // then
        assertThat(result.getPartnerId())
                .isSameAs(partnerId);
        assertThat(result.getPartnerCreatedAt())
                .isSameAs(partnerCreatedAt);
        assertThat(result.isDropship()).isFalse();
        assertThat(result.getStepDataList())
                .as("cached statuses should be complemented by calculated ones, leaving times as is")
                .containsExactly(
                        makeStateStepStatus(WizardStepType.SUPPLIER_INFO, Status.FULL, now.minusSeconds(10L)),
                        makeStateStepStatus(WizardStepType.ASSORTMENT, Status.FILLED)
                );
        then(wizardService)
                .should(description("only non-completed steps should be calculated"))
                .getStatuses(partnerId, EnumSet.complementOf(EnumSet.of(WizardStepType.SUPPLIER_INFO)), true);
        then(partnerOnboardingStateService)
                .should(description("only non-completed steps should be updated"))
                .savePartnerState(partnerIdLong, now, List.of(
                        makeOnboardingMeta("ASSORTMENT", "FILLED")
                ));
    }

    @Test
    void calculatePartnerOnboardingStateCacheFull() {
        // given
        given(partnerOnboardingStateService.getOnboardingStates(eq(partnerId.toLong()), any()))
                .willReturn(
                        Stream.of(WizardStepType.values())
                                .map(step -> makeOnboardingMeta(step.name(), "FULL"))
                                .collect(Collectors.toList())
                );
        given(wizardService.getStatuses(eq(partnerId), any(), eq(true)))
                .willAnswer(invocation -> ((Collection<WizardStepType>) invocation.getArgument(1))
                        .stream()
                        .map(step -> makeWizardStepStatus(step, Status.FULL))
                        .collect(Collectors.toList())
                );

        // when
        var result = calculator.calculatePartnerOnboardingState(
                testCandidate,
                Suppliers.ofInstance(now)
        );

        // then
        assertThat(result.getPartnerId())
                .isSameAs(partnerId);
        assertThat(result.getPartnerCreatedAt())
                .isSameAs(partnerCreatedAt);
        assertThat(result.getStepDataList())
                .as("cached statuses should be complemented by calculated ones")
                .containsExactlyElementsOf(
                        Stream.of(WizardStepType.values())
                                .map(step -> makeStateStepStatus(step, Status.FULL))
                                .collect(Collectors.toList())
                );
        then(wizardService)
                .should(description("only non-completed steps should be calculated"))
                .getStatuses(partnerId, EnumSet.noneOf(WizardStepType.class), true);
        then(partnerOnboardingStateService)
                .should(description("only changed steps should be updated"))
                .savePartnerState(partnerIdLong, now, List.of());
    }

    @Test
    void calculatePartnerOnboardingWithNotChangedStep() {
        // given
        given(partnerOnboardingStateService.getOnboardingStates(eq(partnerId.toLong()), any()))
                .willReturn(List.of(
                        makeOnboardingMeta("ASSORTMENT", "FULL", now.minusSeconds(100L)),
                        makeOnboardingMeta("SUPPLIER_FEED", "EMPTY", now.minusSeconds(10L)),
                        makeOnboardingMeta("ORDER_PROCESSING", "EMPTY", now.minusSeconds(10L))
                ));
        given(wizardService.getStatuses(eq(partnerId), any(), eq(true)))
                .willReturn(List.of(
                        makeWizardStepStatus(WizardStepType.SUPPLIER_FEED, Status.EMPTY),
                        makeWizardStepStatus(WizardStepType.ORDER_PROCESSING, Status.FILLED)
                ));
        given(partnerTypeAwareService.getPartnerTypeAwareInfo(partnerIdLong)).willReturn(notDropshipInfo());

        // when
        var result = calculator.calculatePartnerOnboardingState(
                testCandidate,
                Suppliers.ofInstance(now)
        );

        // then
        assertThat(result.getPartnerId())
                .isSameAs(partnerId);
        assertThat(result.getPartnerCreatedAt())
                .isSameAs(partnerCreatedAt);
        assertThat(result.isDropship()).isFalse();
        assertThat(result.getStepDataList())
                .as("cached statuses should be complemented by calculated ones, leaving times as is")
                .containsExactly(
                        makeStateStepStatus(WizardStepType.ASSORTMENT, Status.FULL, now.minusSeconds(100L)),
                        makeStateStepStatus(WizardStepType.SUPPLIER_FEED, Status.EMPTY, now.minusSeconds(10L)),
                        makeStateStepStatus(WizardStepType.ORDER_PROCESSING, Status.FILLED, now)
                );
        then(wizardService)
                .should(description("only non-completed steps should be calculated"))
                .getStatuses(partnerId, EnumSet.complementOf(EnumSet.of(WizardStepType.ASSORTMENT)), true);
        then(partnerOnboardingStateService)
                .should(description("only changed steps should be updated"))
                .savePartnerState(partnerIdLong, now, List.of(
                        makeOnboardingMeta("ORDER_PROCESSING", "FILLED")
                ));
    }


    private static PartnerOnboardingStateRecord makeOnboardingMeta(String key, String value) {
        return makeOnboardingMeta(key, value, now);
    }

    private static PartnerOnboardingStateRecord makeOnboardingMeta(String key, String value, Instant updateTime) {
        return new PartnerOnboardingStateRecord.Builder()
                .withPartnerId(partnerIdLong)
                .withNamespace(PartnerOnboardingStateParamNamespace.WIZARD)
                .withParamName(key)
                .withParamValue(value)
                .withUpdateDate(updateTime)
                .build();
    }

    private static WizardStepStatus makeWizardStepStatus(WizardStepType step, Status status) {
        return WizardStepStatus.newBuilder()
                .withStatus(status)
                .withStep(step)
                .build();
    }

    private static PartnerOnboardingState.WizardStepData makeStateStepStatus(WizardStepType step, Status status) {
        return makeStateStepStatus(step, status, now);
    }

    private static PartnerOnboardingState.WizardStepData makeStateStepStatus(WizardStepType step, Status status,
                                                                             Instant updateTime) {
        return new PartnerOnboardingState.WizardStepData(step, status, updateTime);
    }

    private ParamService mockParamService() {
        ParamService service = mock(ParamService.class);
        when(service.getOptionalParamNumberValue(ParamType.HOME_REGION, partnerIdLong))
                .thenReturn(Optional.of(new BigDecimal(RUSSIA_ID)));
        when(service.getOptionalParamNumberValue(ParamType.LOCAL_DELIVERY_REGION, partnerIdLong))
                .thenReturn(Optional.of(new BigDecimal(MOSCOW_ID)));

        return service;
    }


}
