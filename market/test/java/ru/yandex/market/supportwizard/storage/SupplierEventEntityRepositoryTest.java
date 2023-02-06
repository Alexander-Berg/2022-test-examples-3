package ru.yandex.market.supportwizard.storage;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.supplier.SupplierEventType;
import ru.yandex.market.supportwizard.base.supplier.SupplierType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SupplierEventEntityRepositoryTest extends BaseFunctionalTest {

    @Autowired
    private SupplierEventRepository tested;

    @Test
    @DbUnitDataSet(after = "supplierEvents.after.csv")
    void testSave() {
        tested.save(createEvent());
    }

    @NotNull
    private SupplierEventEntity createEvent() {
        return SupplierEventEntity.builder()
                .withPartnerId(1L)
                .withPartnerName("Name")
                .withIsPartnerApi(true)
                .withPartnerType(SupplierType.DROPSHIP)
                .withIsClickAndCollect(true)
                .withEventType(SupplierEventType.APPROVAL_REQUEST)
                .withCreationTime(LocalDate.of(2020, 2, 2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .withUpdateTime(LocalDate.of(2020, 2, 3).atStartOfDay().toInstant(ZoneOffset.UTC))
                .withEventTime(LocalDate.of(2020, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();
    }

    @Test
    @DbUnitDataSet(before = "supplierEvents.before.csv")
    void testGetRegistrationEventsWithoutTickets() {
        List<SupplierEventEntity> events = tested.getRegistrationEventsWithoutTickets(5, 25);
        assertNotNull(events);
        assertEquals(3, events.size());
    }
}
