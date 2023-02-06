package ru.yandex.market.core.partner.onboarding.configurator;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.partner.onboarding.sender.PartnerOnboardingNotificationConfigurator;
import ru.yandex.market.core.partner.onboarding.sender.PartnerOnboardingNotificationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PartnerOnboardingNotificationConfiguratorTest {

    @Test
    public void testNormal() {
        PartnerOnboardingNotificationConfigurator configurator = new PartnerOnboardingNotificationConfigurator(List.of(
                new PartnerOnboardingNotificationStep(1, List.of()),
                new PartnerOnboardingNotificationStep(3, List.of()),
                new PartnerOnboardingNotificationStep(2, List.of())
        ));
        List<PartnerOnboardingNotificationStep> sortedSteps = configurator.getAllStepsSortedByDelay();
        assertThat(sortedSteps
                .stream()
                .map(PartnerOnboardingNotificationStep::getDelayInDays))
                .as("Конфигуратор должен возвращать шаги, отсортированные по задержке")
                .containsExactly(1, 2, 3);
    }

    @Test
    public void testNegativeDelay() {
        assertThatThrownBy(() -> {
            new PartnerOnboardingNotificationConfigurator(List.of(
                    new PartnerOnboardingNotificationStep(1, List.of()),
                    new PartnerOnboardingNotificationStep(-2, List.of()),
                    new PartnerOnboardingNotificationStep(3, List.of())
            ));
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDuplicateDelay() {
        assertThatThrownBy(() -> {
            new PartnerOnboardingNotificationConfigurator(List.of(
                    new PartnerOnboardingNotificationStep(1, List.of()),
                    new PartnerOnboardingNotificationStep(2, List.of()),
                    new PartnerOnboardingNotificationStep(3, List.of()),
                    new PartnerOnboardingNotificationStep(4, List.of()),
                    new PartnerOnboardingNotificationStep(3, List.of())
            ));
        }).isInstanceOf(IllegalArgumentException.class);
    }
}
