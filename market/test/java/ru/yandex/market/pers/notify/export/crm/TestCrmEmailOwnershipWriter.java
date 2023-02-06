package ru.yandex.market.pers.notify.export.crm;

import org.apache.log4j.Logger;
import ru.yandex.market.pers.notify.model.EmailOwnership;

import java.util.ArrayList;
import java.util.List;

/**
 * @author apershukov
 */
public class TestCrmEmailOwnershipWriter implements CrmEmailOwnershipWriter {

    private final List<EmailOwnership> ownerships = new ArrayList<>();

    @Override
    public void write(Logger logger, EmailOwnership ownership) {
        ownerships.add(ownership);
    }

    List<EmailOwnership> getOwnerships() {
        return ownerships;
    }
}
