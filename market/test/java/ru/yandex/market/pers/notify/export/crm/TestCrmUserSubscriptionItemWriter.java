package ru.yandex.market.pers.notify.export;

import org.apache.log4j.Logger;
import ru.yandex.market.pers.notify.export.crm.CrmEmailSubscriptionItem;
import ru.yandex.market.pers.notify.export.crm.CrmUserSubscriptionItemWriter;

import java.util.ArrayList;
import java.util.List;

public class TestCrmUserSubscriptionItemWriter implements CrmUserSubscriptionItemWriter {

    private final List<CrmEmailSubscriptionItem> items = new ArrayList<>();

    @Override
    public void writeItem(Logger log, CrmEmailSubscriptionItem item) {
        items.add(item);
    }

    public CrmEmailSubscriptionItem getItem(int index) {
        return items.get(index);
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
