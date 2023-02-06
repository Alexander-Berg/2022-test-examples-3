package ru.yandex.market.pvz.core.domain.vaccination;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestVaccinationPickupPointFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class VaccinationEmployeeCommandServiceTest {

    private final TestVaccinationPickupPointFactory vaccinationPickupPointFactory;

    private final VaccinationEmployeeCommandService vaccinationEmployeeCommandService;
    private final VaccinationPickupPointQueryService vaccinationPickupPointQueryService;

    @Test
    void markCertificateValidTest() {
        var vaccinationPickupPoint = vaccinationPickupPointFactory.create();
        assertThat(vaccinationPickupPoint.getEmployees()).isNotEmpty();
        var employee = vaccinationPickupPoint.getEmployees().get(0);
        assertThat(employee.getId()).isNotNull();

        var actual = vaccinationEmployeeCommandService.markCertificateValid(employee.getId());

        assertThat(actual.getVerified()).isTrue();

        var verified = vaccinationPickupPointQueryService.get(vaccinationPickupPoint.getId());

        assertThat(verified).isPresent();
        assertThat(verified.get().getEmployees().get(0).getVerified()).isTrue();
    }

    @Test
    void tryToMarkCertificateValidForNotExistentEmployee() {
        assertThatThrownBy(() -> vaccinationEmployeeCommandService.markCertificateValid(99L))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void markCertificateInvalidTest() {
        var vaccinationPickupPoint = vaccinationPickupPointFactory.create();
        assertThat(vaccinationPickupPoint.getEmployees()).isNotEmpty();
        var employee = vaccinationPickupPoint.getEmployees().get(0);
        assertThat(employee.getId()).isNotNull();

        String failReason = "Все не то, все не так";
        var actual = vaccinationEmployeeCommandService.markCertificateInvalid(employee.getId(), failReason);

        assertThat(actual.getVerified()).isFalse();
        assertThat(actual.getVerifyFailReason()).isEqualTo(failReason);

        var verified = vaccinationPickupPointQueryService.get(vaccinationPickupPoint.getId());

        assertThat(verified).isPresent();
        assertThat(verified.get().getEmployees().get(0).getVerified()).isFalse();
        assertThat(verified.get().getEmployees().get(0).getVerifyFailReason()).isEqualTo(failReason);
    }

    @Test
    void tryToMarkCertificateInvalidForNotExistentEmployee() {
        assertThatThrownBy(() -> vaccinationEmployeeCommandService.markCertificateInvalid(99L, "все не то"))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }
}
