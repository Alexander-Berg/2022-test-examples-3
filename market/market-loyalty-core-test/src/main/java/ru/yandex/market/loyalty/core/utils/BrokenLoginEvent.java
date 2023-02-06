package ru.yandex.market.loyalty.core.utils;

import java.util.List;
import java.util.Map;

import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventPersistentDataId;
import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.trigger.EventParamName;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventType;
import ru.yandex.market.loyalty.core.model.trigger.event.data.TriggerEventPersistentData;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class BrokenLoginEvent extends LoginEvent {
    public BrokenLoginEvent(
            Long id,
            Map<EventParamName<?>, GenericParam<?>> params,
            List<TriggerEventPersistentData> persistentData,
            List<TriggerEventPersistentDataId> persistentDataIds,
            int processTryCount,
            TriggerEventProcessedResult processedResult,
            boolean isPartitionedEvent
    ) {
        super(id, params, persistentData, persistentDataIds, processTryCount, processedResult, isPartitionedEvent);
    }

    @Override
    public TriggerEventType<?> getEventType() {
        return TriggersFactory.BROKEN_LOGIN;
    }
}
