package ru.yandex.market.partner.onboarding;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.logbroker.OnboardingStepResultOuterClass;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.onboarding.finder.DbPartnerOnboardingFinder;
import ru.yandex.market.core.partner.onboarding.listeners.PartnerOnboardingListener;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingProtoLBEvent;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingStateService;
import ru.yandex.market.core.partner.onboarding.state.WizardBasedPartnerOnboardingStateCalculator;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.WizardService;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartnerOnboardingExecutorFunctionalTest extends FunctionalTest {

    private static final int CALCULATION_PERIOD_MINUTES = 30;

    @Autowired
    private DbPartnerOnboardingFinder finder;

    @Autowired
    private PartnerOnboardingStateService stateService;

    @Autowired
    private PartnerTypeAwareService typeAwareService;

    @Autowired
    private ParamService paramService;

    @Autowired
    @Qualifier("onboardingExecutorService")
    private ExecutorService onboardingExecutorService;

    @Autowired
    private Supplier<Boolean> partnerOnboardingUseSingleThread;

    @Autowired
    private Supplier<Long> partnerOnboardingCalculationPeriod;

    @Autowired
    private LogbrokerEventPublisher<PartnerOnboardingProtoLBEvent> logbrokerOnboardingEventPublisher;

    @Autowired
    private PartnerOnboardingPublisherImpl partnerOnboardingPublisher;
    @Autowired
    private PartnerPlacementProgramService programService;


    private WizardService wizardServiceMock = mock(WizardService.class);
    private PartnerOnboardingListener dummyListener = mock(PartnerOnboardingListener.class);

    private PartnerOnboardingExecutor executor;
    private WizardBasedPartnerOnboardingStateCalculator calculator;

    @BeforeEach
    public void init() {
        calculator = new WizardBasedPartnerOnboardingStateCalculator(
                stateService,
                typeAwareService,
                wizardServiceMock,
                paramService,
                partnerOnboardingPublisher,
                programService
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerOnboardingExecutorFunctionalTest.before.csv",
            after = "PartnerOnboardingExecutorFunctionalTest.after.csv"
    )
    public void testJob() {
        //given
        given(wizardServiceMock.getStatuses(any(), any(), eq(true))).willReturn(dummyWizardStatuses());
        executor = new PartnerOnboardingExecutor(
                CALCULATION_PERIOD_MINUTES,
                finder,
                calculator,
                List.of(dummyListener),
                onboardingExecutorService,
                partnerOnboardingUseSingleThread,
                partnerOnboardingCalculationPeriod
        );

        //when
        when(logbrokerOnboardingEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> {
                    PartnerOnboardingProtoLBEvent event = invocation.getArgument(0);
                    return CompletableFuture.completedFuture(event);
                });
        executor.doJob(null);

        //then
        ArgumentCaptor<Collection<PartnerOnboardingState>> stateCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(dummyListener, times(1)).onOnboardingPartnersFound(stateCaptor.capture());
        Collection<PartnerOnboardingState> stateList = stateCaptor.getValue();
        assertThat(stateList.size()).isEqualTo(2);
        var partnerIds = stateList.stream()
                .map(PartnerOnboardingState::getPartnerIdAsLong).distinct().collect(Collectors.toList());
        assertThat(partnerIds.size()).isEqualTo(2);
        verify(logbrokerOnboardingEventPublisher, times(6)).publishEventAsync(any());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerOnboardingExecutorFunctionalTest.before.csv"
    )
    public void testJobFailPublish() {
        //given
        given(wizardServiceMock.getStatuses(any(), any(), eq(true))).willReturn(dummyWizardStatuses());
        executor = new PartnerOnboardingExecutor(
                CALCULATION_PERIOD_MINUTES,
                finder,
                calculator,
                List.of(dummyListener),
                onboardingExecutorService,
                partnerOnboardingUseSingleThread,
                partnerOnboardingCalculationPeriod
        );

        CompletableFuture<PartnerOnboardingProtoLBEvent> exceptionalFuture = new CompletableFuture<>();
        exceptionalFuture.completeExceptionally(new RuntimeException());

        //when
        when(logbrokerOnboardingEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> {
                    PartnerOnboardingProtoLBEvent event = invocation.getArgument(0);
                    if (event.getPayload().getStep() ==
                            OnboardingStepResultOuterClass.OnboardingStepResult.WizardStep.ASSORTMENT) {
                        return exceptionalFuture;
                    }
                    return CompletableFuture.completedFuture(event);
                });
        Assertions.assertThrows(RuntimeException.class, () -> executor.doJob(null),
                "Failed to send 2 onboarding events to logbroker");
    }

    private List<WizardStepStatus> dummyWizardStatuses() {
        return List.of(
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.SUPPLIER_INFO)
                        .withStatus(Status.FULL)
                        .build(),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.ASSORTMENT)
                        .withStatus(Status.EMPTY)
                        .build()
        );
    }
}
