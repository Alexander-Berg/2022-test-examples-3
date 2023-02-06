package ru.yandex.market.supportwizard.storage;

import java.time.Instant;
import java.time.LocalDate;

import com.google.common.collect.Sets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ReflectionAssertMatcher;
import ru.yandex.market.supportwizard.base.supplier.SupplierOnboardingStepType;
import ru.yandex.market.supportwizard.base.supplier.SupplierType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.storage.jsonb.SupplierOnboardingStepData;
import ru.yandex.market.supportwizard.storage.jsonb.SupplierOnboardingTicketData;

import static java.util.Collections.singletonList;

public class SupplierOnboardingAlertTicketRepositoryTest extends BaseFunctionalTest {

    @Autowired
    private SupplierOnboardingAlertTicketRepository tested;

    @DbUnitDataSet(after = "onboardingTickets.csv")
    @Test
    void testSave() {
        tested.save(createTestTicketAlert());
    }

    @DbUnitDataSet(before = "onboardingTickets.csv")
    @Test
    void testRead() {
        MatcherAssert.assertThat(tested.findAll(),
                new ReflectionAssertMatcher<>(singletonList(createTestTicketAlert())));
    }

    private SupplierOnboardingAlertTicketEntity createTestTicketAlert() {
        return SupplierOnboardingAlertTicketEntity.builder()
                .withId("ONBOARDING-1")
                .withStep(SupplierOnboardingStepType.REGISTRATION)
                .withDate(LocalDate.of(2020, 8, 5))
                .withTicketData(SupplierOnboardingTicketData.builder()
                        .withStuckOnStep(Sets.newHashSet(SupplierOnboardingStepData.builder()
                                .withStartTime(Instant.ofEpochMilli(1502713067L))
                                .withEndTime(Instant.ofEpochMilli(1602713067L))
                                .withSupplierId(20L)
                                .withSupplierName("Kotiki")
                                .withPartnerType(SupplierType.DROPSHIP)
                                .withIsClickAndCollect(true)
                                .withIsPartnerApi(true)
                                .build()))
                        .withFinishedStep(Sets.newHashSet(SupplierOnboardingStepData.builder()
                                .withStartTime(Instant.ofEpochMilli(1502713067L))
                                .withSupplierId(21L)
                                .withSupplierName("Kotiki2")
                                .withPartnerType(SupplierType.FULFILLMENT)
                                .build()))
                        .build())
                .build();
    }
}
