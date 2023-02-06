package ru.yandex.market.hrms.tms.manager.medical_examination;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.MedicalExaminationCalculationManager;

public class MedicalExamDateCalculationTest extends AbstractTmsTest {

    @Autowired
    private MedicalExaminationCalculationManager manager;

    @DbUnitDataSet(before = "MedicalExamDateCalculationTest.before.csv",
            after = "MedicalExamDateCalculationTest.after.csv")
    @Test
    void calculateMedExamDates() {
        mockClock(LocalDate.of(2022, 6, 1));

        manager.calculateDatesForDomain(1L);
    }

    @DbUnitDataSet(before = {"MedicalExamDateCalculationTest.before.csv",
            "MedicalExamDateCalculationTest.withSchedules.before.csv"},
            after = "MedicalExamDateCalculationTest.withSchedules.after.csv")
    @Test
    void calculateMedExamDatesWithSchedulesTest() {
        mockClock(LocalDate.of(2022, 6, 1));

        manager.calculateDatesForDomain(1L);
    }

    @DbUnitDataSet(before = {"MedicalExamDateCalculationTest.referral.before.csv"},
            after = "MedicalExamDateCalculationTest.referral.after.csv")
    @Test
    void buildMedicalReferralTest() {
        mockClock(LocalDate.of(2022, 6, 1));
        manager.buildMedicalExaminationReferral();
    }
}
