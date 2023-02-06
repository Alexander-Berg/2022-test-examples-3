package ru.yandex.market.mbo.billing.custom;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.gwt.models.billing.CustomAction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BillingCustomActionsServiceMock extends BillingCustomActionsService {

    private List<CustomAction> actions = new ArrayList<>();

    @Override
    public CustomAction saveAction(CustomAction action, long createdBy) {
        action.setCreatorId(createdBy);
        actions.add(action);
        return action;
    }

    public List<CustomAction> getActionInInterval(Pair<Calendar, Calendar> interval) {
        return actions;
    }
}
