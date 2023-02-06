package ru.yandex.market.core.partner.onboarding.sender.delay;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.calendar.impl.CalendarCalculationService;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.TimezoneService;
import ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.RUSSIA_ID;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.dropshipInfo;

/**
 * Калькулятор, считающий задержку подключения партнера, учитывая только рабочее время
 */
@DbUnitDataSet(before = "WorkingTimeDelayCalculatorFunctionalTest.before.csv")
class WorkingTimeDelayCalculatorFunctionalTest extends FunctionalTest {
    private WorkingTimeDelayCalculator delayCalc;

    private static final Instant now = Instant.now();
    private static final Instant oneDayAgo = now.minus(Duration.ofDays(1));
    private static final Instant twoDaysAgo = now.minus(Duration.ofDays(2));

    @Autowired
    private RegionService regionService;

    @Autowired
    private TimezoneService timezoneService;

    private static final long OK_REGION_ID = 1;
    private static final long NULL_TZ_REGION_ID = 2;

    private final CalendarCalculationService calculationService = mock(CalendarCalculationService.class);

    @BeforeEach
    void init() {
        delayCalc = new WorkingTimeDelayCalculator(
                calculationService,
                regionService,
                timezoneService
        );
    }

    @Test
    void testDefaultCalculation() {
        //given
        given(calculationService.calculateWorkingTimeDuration(
                eq(RUSSIA_ID),
                eq(ZoneId.of("Europe/Moscow")),
                any(),
                any()
        )).willReturn(Duration.ofDays(1));

        PartnerOnboardingState state = createDummyState(RUSSIA_ID, OK_REGION_ID);

        //when
        Duration delay = delayCalc.calculateOnboardingDelayInDays(state, () -> now);

        //then
        assertThat(delay.toDays()).isEqualTo(1);
    }

    @Test
    void testNullTimeZone() {
        PartnerOnboardingState state = createDummyState(RUSSIA_ID, NULL_TZ_REGION_ID);

        //when
        delayCalc.calculateOnboardingDelayInDays(state, () -> now);

        //then
        //Временная зона для тестов по умолчанию - владивосток, поэтому должна подставиться она
        verify(calculationService, times(1)).calculateWorkingTimeDuration(
                eq(RUSSIA_ID), eq(ZoneId.of("Asia/Vladivostok")), any(), any());
    }

    private PartnerOnboardingState createDummyState(long countryId, long localRegionId) {
        return PartnerOnboardingState.builder()
                .withPartnerId(PARTNER_ID_ENTITY)
                .withPartnerName("TestDropship")
                .withCampaignId(SenderTestUtils.CAMPAIGN_ID)
                .withPartnerTypeAwareInfo(dropshipInfo())
                .withCountryId(countryId)
                .withLocalRegionId(localRegionId)
                .withPartnerCreatedAt(twoDaysAgo)
                .withStepDataList(List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                twoDaysAgo
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                twoDaysAgo
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                oneDayAgo
                        )
                ))
                .withHasPublishError(false)
                .withActualWizardStepsCompleted(true)
                .build();
    }
}
