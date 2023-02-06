package ru.yandex.market.pvz.core.domain.vaccination;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class VaccinationPickupPointCommandServiceTest {

    private final VaccinationPickupPointCommandService vaccinationPickupPointCommandService;
    private final VaccinationPickupPointQueryService vaccinationPickupPointQueryService;

    @Test
    public void create() {
        var employee1 = VaccinationEmployeeParams.builder()
                .name("Иванов Петр Федорович")
                .passport("4514 208985")
                .nationality("русский")
                .birthday(LocalDate.of(1988, 7, 17))
                .firstVaccinationDate(LocalDate.of(2021, 7, 13))
                .secondVaccinationDate(LocalDate.of(2021, 8, 4))
                .certificate("1249384752834923")
                .signed(true);
        var employee2 = VaccinationEmployeeParams.builder()
                .name("Гаргамедов Ильзар Фехтарович")
                .passport("9988 133949")
                .nationality("киргиз")
                .birthday(LocalDate.of(1976, 2, 11))
                .firstVaccinationDate(LocalDate.of(2021, 7, 3))
                .secondVaccinationDate(LocalDate.of(2021, 7, 25))
                .certificate("1249385752834924")
                .signed(true);
        var vaccinationPickupPoint = VaccinationPickupPointParams.builder()
                .id(1L)
                .partnerName("ООО Ромашка")
                .region("Москва и Московская область")
                .city("Зеленоград")
                .address("ул. Роз, д.5, к.1")
                .amount((short) 6)
                .vaccinatedAmount((short) 3)
                .created(OffsetDateTime.of(LocalDateTime.of(2021, 8, 3, 11, 36, 10), OffsetDateTime.now().getOffset()));

        var created = vaccinationPickupPointCommandService.create(
                vaccinationPickupPoint.employees(List.of(employee1.build(), employee2.build())).build());
        assertThat(created.getEmployees()).hasSize(2);
        Long employeeId1 = created.getEmployees().get(0).getId();
        Long employeeId2 = created.getEmployees().get(1).getId();

        var actual = vaccinationPickupPointQueryService.get(1L);
        assertThat(actual).isPresent();

        var expected = vaccinationPickupPoint
                .employees(List.of(
                        employee1.id(employeeId1).build(),
                        employee2.id(employeeId2).build()
                ))
                .build();

        assertThat(actual.get()).isEqualTo(expected);
    }

    @Test
    public void createExistent() {
        var employee1 = VaccinationEmployeeParams.builder()
                .name("Иванов Петр Федорович")
                .passport("4514 208985")
                .nationality("русский")
                .birthday(LocalDate.of(1988, 7, 17))
                .firstVaccinationDate(LocalDate.of(2021, 7, 13))
                .secondVaccinationDate(LocalDate.of(2021, 8, 4))
                .certificate("1249384752834923")
                .signed(true);
        var employee2 = VaccinationEmployeeParams.builder()
                .name("Гаргамедов Ильзар Фехтарович")
                .passport("9988 133949")
                .nationality("киргиз")
                .birthday(LocalDate.of(1976, 2, 11))
                .firstVaccinationDate(LocalDate.of(2021, 7, 3))
                .secondVaccinationDate(LocalDate.of(2021, 7, 25))
                .certificate("1249385752834924")
                .signed(true);
        var vaccinationPickupPoint = VaccinationPickupPointParams.builder()
                .id(1L)
                .partnerName("ООО Ромашка")
                .region("Москва и Московская область")
                .city("Зеленоград")
                .address("ул. Роз, д.5, к.1")
                .amount((short) 6)
                .vaccinatedAmount((short) 3)
                .created(OffsetDateTime.of(LocalDateTime.of(2021, 8, 3, 11, 36, 10), OffsetDateTime.now().getOffset()));

        vaccinationPickupPointCommandService.create(
                vaccinationPickupPoint.employees(List.of(employee1.build(), employee2.build())).build());
        vaccinationPickupPointCommandService.create(
                vaccinationPickupPoint.employees(List.of(employee1.build(), employee2.build())).build());
        vaccinationPickupPointCommandService.create(
                vaccinationPickupPoint.employees(List.of(employee1.build(), employee2.build())).build());

        assertThat(vaccinationPickupPointQueryService.getAll()).hasSize(1);
    }

}
