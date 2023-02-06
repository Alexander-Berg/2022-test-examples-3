package ru.yandex.market.delivery.transport_manager.service.health;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.service.health.stress.StressPrerequisitesChecker;

@DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
@DatabaseSetup("/repository/transportation/multiple_transportations.xml")
public class StressPrerequisiteCheckerTest extends AbstractContextualTest {

    @Autowired
    private StressPrerequisitesChecker healthChecker;

    @DatabaseSetup("/repository/transportation/register.xml")
    @DatabaseSetup("/repository/transportation/register_meta.xml")
    @Test
    void ready() {
        mockProperty(TmPropertyKey.STRESS_TEST_REGISTER_MIN_SIZE, 1);
        mockProperty(TmPropertyKey.STRESS_TEST_REGISTER_ID, 1L);
        mockProperty(TmPropertyKey.STRESS_TEST_TRANSPORTATIONS_MIN_AMOUNT, 2);
        softly.assertThat(healthChecker.enoughTransportations()).isEqualTo("0;OK");
        softly.assertThat(healthChecker.largeRegisterExistsAndIsLarge()).isEqualTo("0;OK");
    }

    @DatabaseSetup("/repository/transportation/register.xml")
    @DatabaseSetup("/repository/transportation/register_meta.xml")
    @Test
    void notReadyRegister() {
        mockProperty(TmPropertyKey.STRESS_TEST_REGISTER_MIN_SIZE, 10000);
        mockProperty(TmPropertyKey.STRESS_TEST_REGISTER_ID, 1L);
        softly.assertThat(healthChecker.largeRegisterExistsAndIsLarge())
            .isEqualTo("2;Register 1 does not exist or has less than 10000 (3) units");
    }

    @Test
    void notReadyRegisterNoRegister() {
        mockProperty(TmPropertyKey.STRESS_TEST_REGISTER_MIN_SIZE, 10000);
        mockProperty(TmPropertyKey.STRESS_TEST_REGISTER_ID, 1L);
        softly.assertThat(healthChecker.largeRegisterExistsAndIsLarge())
            .isEqualTo("2;Register 1 does not exist or has less than 10000 (0) units");
    }

    @Test
    void notReadyTransportation() {
        mockProperty(TmPropertyKey.STRESS_TEST_TRANSPORTATIONS_MIN_AMOUNT, 200);
        softly.assertThat(healthChecker.enoughTransportations())
            .isEqualTo("2;Not enough transportations for stress test: 9/200");
    }

}
