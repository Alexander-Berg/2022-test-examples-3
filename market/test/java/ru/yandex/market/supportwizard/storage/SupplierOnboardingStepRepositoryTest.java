package ru.yandex.market.supportwizard.storage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.supplier.SupplierOnboardingStepType;
import ru.yandex.market.supportwizard.base.supplier.SupplierType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;

public class SupplierOnboardingStepRepositoryTest extends BaseFunctionalTest {

    @Autowired
    private SupplierOnboardingStepRepository tested;

    @Test
    @DbUnitDataSet(after = "supplierRegistrationSteps.after.csv")
    void testSave() {
        tested.save(createStep());
    }

    @NotNull
    private SupplierOnboardingStepEntity createStep() {
        return SupplierOnboardingStepEntity.builder()
                .withId(1L)
                .withPartnerId(1L)
                .withPartnerName("Name")
                .withIsPartnerApi(true)
                .withPartnerType(SupplierType.DROPSHIP)
                .withIsClickAndCollect(true)
                .withRegistrationStep(SupplierOnboardingStepType.REQUEST_PROCESSING)
                .withStartTime(ZonedDateTime.of(LocalDate.of(2020, 2, 2), LocalTime.of(0,0), ZoneOffset.UTC).toInstant())
                .withEndTime(ZonedDateTime.of(LocalDate.of(2020, 2, 3), LocalTime.of(0,0), ZoneOffset.UTC).toInstant())
                .build();
    }
}
