package ru.yandex.market.hrms.core.service.vaccination;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.vaccination.VaccinationService;
import ru.yandex.market.hrms.core.domain.vaccination.repo.Vaccination;
import ru.yandex.market.hrms.core.domain.vaccination.repo.VaccinationDetail;
import ru.yandex.market.hrms.model.view.vaccination.VaccinationViewV2;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

public class VaccinationServiceTest extends AbstractCoreTest {

    @Autowired
    private VaccinationService vaccinationService;

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.Populate.before.csv",
            after = "VaccinationServiceTest.Populate.after.csv")
    void populate() {
        vaccinationService.populate();
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredByDomainId() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                2L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(5L);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredByGroupId() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                1L,
                86L,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredByEmployeeName() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                1L,
                null,
                "АЛЕКСАНДР",
                null,
                null,
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredByPosition() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                1L,
                null,
                null,
                "кладовщик",
                null,
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredByPlanDate1() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                1L,
                null,
                null,
                null,
                "2021-08-01,2021-08-31",
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredByFactDate1() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                1L,
                null,
                null,
                null,
                null,
                "2021-07-01,2021-07-31",
                null,
                null,
                true,
                PageRequest.of(0, 10, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(1L, 2L, 4L);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredIfShowFullyVaccinated() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                PageRequest.of(0, 10, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.GetVaccinationsFiltered.before.csv")
    void findAllFilteredPaginationCheck() {
        mockClock(LocalDate.of(2021, 8, 10));
        Page<VaccinationDetail> vaccinations = vaccinationService.findAllFilteredV2(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(1, 2, Sort.unsorted())
        );
        Assertions.assertThat(vaccinations.map(VaccinationDetail::getEmployeeId))
                .containsExactlyInAnyOrder(3L);
    }

    /**
     * первая (ночная) проверка
     */
    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.CheckVaccinations.before.csv",
            after = "VaccinationServiceTest.FirstCheckVaccinations.after.csv")
    void checkVaccinationsNighttime() {
        mockClock(LocalDateTime.of(2021, 7, 23, 1, 10));
        vaccinationService.checkVaccinations(true);
    }

    /**
     * вторая (дневная) проверка
     */
    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.CheckVaccinations.before.csv",
            after = "VaccinationServiceTest.SecondCheckVaccinations.after.csv")
    void checkVaccinationsDaytime() {
        mockClock(LocalDateTime.of(2021, 7, 23, 13, 10));
        vaccinationService.checkVaccinations(false);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetFactDate.before.csv")
    void setFactDate1WhenFactDate2NotNull() {
        mockClock(LocalDate.of(2021, 7, 1));
        Assertions.assertThatThrownBy(() -> vaccinationService.setFactDate(1, 1))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetFactDate.before.csv")
    void setFactDate2WhenFactDate1Null() {
        mockClock(LocalDate.of(2021, 7, 1));
        Assertions.assertThatThrownBy(() -> vaccinationService.setFactDate(3, 2))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetFactDate.before.csv",
            after = "VaccinationServiceTest.SetFactDate1HasNotNightShift.after.csv")
    void setFactDate1HasNotNightShift() {
        mockClock(LocalDate.of(2021, 7, 1));
        vaccinationService.setFactDate(4, 1, LocalDate.of(2021, 7, 1));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetFactDate.before.csv",
            after = "VaccinationServiceTest.SetFactDate1HasNightShift.after.csv")
    void setFactDate1HasNightShift() {
        mockClock(LocalDate.of(2021, 7, 1));
        vaccinationService.setFactDate(3, 1, LocalDate.of(2021, 7, 1));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetFactDate.before.csv",
            after = "VaccinationServiceTest.SetFactDate2.after.csv")
    void setFactDate2() {
        mockClock(LocalDate.of(2021, 7, 1));
        vaccinationService.setFactDate(2, 2, LocalDate.of(2021, 7, 1));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.CheckVaccinationsV2.before.csv",
            after = "VaccinationServiceTest.FirstCheckVaccinationsV2.after.csv")
    void checkVaccinationsV2FirstCheck() {
        mockClock(LocalDateTime.of(2021, 8, 25, 1, 10));
        vaccinationService.checkVaccinationsV2(true);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.CheckVaccinationsV2.before.csv",
            after = "VaccinationServiceTest.SecondCheckVaccinationsV2.after.csv")
    void checkVaccinationsV2SecondCheck() {
        mockClock(LocalDateTime.of(2021, 8, 25, 13, 10));
        vaccinationService.checkVaccinationsV2(false);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.CheckVaccinationsV2FirstDayOfMonth.before.csv",
            after = "VaccinationServiceTest.CheckVaccinationsV2FirstDayOfMonth.after.csv")
    void checkVaccinationsV2FirstCheckFirstDayOfMonth() {
        mockClock(LocalDateTime.of(2021, 10, 1, 1, 10));
        vaccinationService.checkVaccinationsV2(true);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetVaccination.before.csv")
    void setVaccinationFactDate1NullAndFactDate2NotNull() {
        mockClock(LocalDateTime.of(2021, 8, 25, 9, 0));
        Assertions.assertThatThrownBy(() -> vaccinationService.setVaccination(
                VaccinationViewV2.builder()
                        .id(1L)
                        .medicalOutlet(null)
                        .dateFact1(null)
                        .dateFact2(LocalDate.of(2021, 8, 25))
                        .build()
        )).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetVaccination.before.csv")
    void setVaccinationDifferenceBetweenFactDate2AndFactDate1LessThan21() {
        mockClock(LocalDateTime.of(2021, 8, 25, 9, 0));
        Assertions.assertThatThrownBy(() -> vaccinationService.setVaccination(
                VaccinationViewV2.builder()
                        .id(1L)
                        .medicalOutlet(null)
                        .dateFact1(LocalDate.of(2021, 8, 25))
                        .dateFact2(LocalDate.of(2021, 8, 25))
                        .build()
        )).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetVaccination.before.csv")
    void setVaccinationFactDate1() {
        mockClock(LocalDateTime.of(2021, 8, 3, 9, 0));
        vaccinationService.setVaccination(
                VaccinationViewV2.builder()
                        .id(1L)
                        .medicalOutlet(null)
                        .dateFact1(LocalDate.of(2021, 8, 3))
                        .dateFact2(null)
                        .build()
        );
        Optional<Vaccination> vaccination = vaccinationService.findById(1L);
        Assertions.assertThat(vaccination).isPresent();
        Assertions.assertThat(vaccination.get().getPlanDate2()).isEqualTo(LocalDate.of(2021, 8, 31));
    }

    @Test
    @DbUnitDataSet(before = "VaccinationServiceTest.SetVaccination.before.csv")
    void setVaccinationMedicalOutlet() {
        mockClock(LocalDateTime.of(2021, 8, 25, 9, 0));
        vaccinationService.setVaccination(
                VaccinationViewV2.builder()
                        .id(1L)
                        .medicalOutlet(LocalDate.of(2021, 9, 1))
                        .dateFact1(LocalDate.of(2021, 8, 3))
                        .dateFact2(null)
                        .build()
        );
        Optional<Vaccination> vaccination = vaccinationService.findById(1L);
        Assertions.assertThat(vaccination).isPresent();
        Assertions.assertThat(vaccination.get().getPlanDate2()).isNull();
    }
}
