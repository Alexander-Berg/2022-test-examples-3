package ru.yandex.market.mbo.core.audit;

import ru.yandex.market.mbo.history.EntityType;
import ru.yandex.market.mbo.history.model.EntityHistoryEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class OracleAuditServiceStub extends OracleAuditService {

    List<EntityHistoryEntry> entities;

    public OracleAuditServiceStub() {
        super(null, null);
        entities = new ArrayList<>();
    }

    public void addEntity(EntityHistoryEntry entry) {
        entities.add(entry);
    }

    @Override
    public List<EntityHistoryEntry> getHistoryLog(
            EntityType entityType,
            Date from,
            Date to,
            boolean needSnapshots,
            String condition) {
        return entities.stream()
                .filter(entry -> entry.getEntityType().equals(entityType))
                .collect(Collectors.toList());
    }

}
