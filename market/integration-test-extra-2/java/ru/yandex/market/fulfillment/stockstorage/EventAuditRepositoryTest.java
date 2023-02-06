package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.EventType;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.PayloadType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.EventAudit;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Korobyte;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.EventAuditRepository;
import ru.yandex.market.fulfillment.stockstorage.service.audit.EventAuditFieldConstants;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class EventAuditRepositoryTest extends AbstractContextualTest {

    @Autowired
    private EventAuditRepository eventAuditRepository;

    /**
     * DBUnit не умеет проверять поля JSONB, поэтому просто проверяем, что появляется запись с правильным типом
     */
    @Test
    @ExpectedDatabase(value = "classpath:database/expected/event_audit/event_added.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void auditEventSave() {
        EventAudit eventAudit = new EventAudit();
        eventAudit.setRequestId("1525433808261/79618b8f6d70516a3facfe4c14824059");
        eventAudit.setType(EventType.SKU_CREATED);
        eventAudit.setTargetType(PayloadType.SKU);
        eventAudit.setTargetId("11235813");
        eventAudit.setPayload(ImmutableMap.of(
                EventAuditFieldConstants.UNIT_ID.getValue(), new UnitId("SKU", 123L, 1),
                EventAuditFieldConstants.KOROBYTE.getValue(), new Korobyte()
        ));
        eventAuditRepository.save(eventAudit);
    }
}
