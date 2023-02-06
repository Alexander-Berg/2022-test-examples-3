package ru.yandex.market.pers.notify.export.crm;

import org.apache.log4j.Logger;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author apershukov
 */
public class TestCrmMobileAppInfoTskvWriter implements CrmMobileAppInfoTskvWriter {

    private final List<MobileAppInfo> items = new ArrayList<>();

    @Override
    public void write(Logger logger, MobileAppInfo item) {
        items.add(item);
    }

    public List<MobileAppInfo> getItems() {
        return items;
    }
}
