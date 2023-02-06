package ru.yandex.market.hrms.tms.executor.startrek;

import java.net.URI;

import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.StatusRef;

public class OpenStatusRef extends StatusRef {
    public OpenStatusRef(long id, URI self, String key, String display, Session session) {
        super(id, self, key, display, session);
    }
}
