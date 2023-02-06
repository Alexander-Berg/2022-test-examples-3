package ru.yandex.market.mboc.common.dict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mboc.common.infrastructure.sql.AuditWriter;

public class AuditWriterMock implements AuditWriter {
    private Map<Long, List<MboAudit.MboAction.Builder>> actionStore = new HashMap<>();

    @Override
    public void writeActions(Collection<MboAudit.MboAction.Builder> actions) {
        actions.forEach(action -> actionStore.compute(
                action.getEntityId(),
                (k, v) -> {
                    if (Objects.isNull(v)) {
                        return new ArrayList<>(List.of(action));
                    } else {
                        v.add(action);
                        return v;
                    }
                }
            )
        );
    }

    public List<MboAudit.MboAction.Builder> getActions(Long offerId) {
        return actionStore.getOrDefault(offerId, List.of());
    }

    public int getActionCount() {
        return actionStore.values().size();
    }

    public int getActionCount(Long offerId) {
        return getActions(offerId).size();
    }

    public void reset() {
        actionStore = Map.of();
    }
}
