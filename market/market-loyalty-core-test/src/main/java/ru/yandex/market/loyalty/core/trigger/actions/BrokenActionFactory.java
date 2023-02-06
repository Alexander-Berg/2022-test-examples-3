package ru.yandex.market.loyalty.core.trigger.actions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.trigger.TriggerAction;
import ru.yandex.market.loyalty.core.model.trigger.event.EventWithIdentity;
import ru.yandex.market.loyalty.core.model.trigger.event.data.TriggerEventData;
import ru.yandex.market.loyalty.core.service.trigger.EventHandleMode;
import ru.yandex.market.loyalty.core.service.trigger.EventHandleRestrictionType;
import ru.yandex.market.loyalty.core.trigger.BaseTriggerPartFactory;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@NoArgsConstructor
@Component(BrokenActionFactory.BROKEN_ACTION_FACTORY)
public class BrokenActionFactory extends BaseTriggerPartFactory
        implements TriggerActionFactory<BrokenActionFactory.BrokenAction> {
    public static final String BROKEN_ACTION_FACTORY = "BrokenActionFactory";

    private static final ConcurrentMap<Long, Boolean> NOT_FAIL_FOR_PROMO = new ConcurrentHashMap<>();

    public static void cleanUp() {
        NOT_FAIL_FOR_PROMO.clear();
    }

    public static void notFailForPromo(long promoId) {
        NOT_FAIL_FOR_PROMO.put(promoId, false);
    }

    @Override
    public BrokenAction create(Long partId, String body) {
        return new BrokenAction(partId);
    }

    @Override
    public String createBody(BrokenAction action) {
        return "";
    }

    public static class BrokenAction extends TriggerAction<EventWithIdentity> {
        private BrokenAction(Long id) {
            super(id);
        }

        @Override
        public Class<EventWithIdentity> getEventClass() {
            return EventWithIdentity.class;
        }

        @Override
        public ProcessResult processEvent(
                EventWithIdentity event,
                TriggerEventData<? extends EventWithIdentity> params,
                EventHandleMode mode, EventHandleRestrictionType restrictionType,
                BudgetMode budgetMode
        ) {
            if (NOT_FAIL_FOR_PROMO.getOrDefault(params.getPromoId(), true)) {
                throw new RuntimeException("As planned");
            }
            return EmptyProcessResult.getInstance();
        }
    }
}
