package ru.yandex.market.mbo.tms.report;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BillingActionColourMock extends BillingActionColour {

    private Set<Long> blueIds = new HashSet<>();

    public void addBlueIds(Collection<Long> ids) {
        blueIds.addAll(ids);
    }

    @Override
    public Colour getActionColourCount(OperatorResourcesReport.DbUnit action, Context context) {
        if (context.getComputableActionClasses().contains(action.inferredActionClass)) {
            return blueIds.contains(action.sourceId) ?
                new Colour().setBlues(action.count) :
                new Colour().setWhites(action.count);
        }
        return super.getActionColourCount(action, context);
    }
}
