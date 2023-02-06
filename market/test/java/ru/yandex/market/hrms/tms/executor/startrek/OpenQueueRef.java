package ru.yandex.market.hrms.tms.executor.startrek;

import java.net.URI;

import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.QueueRef;

public class OpenQueueRef extends QueueRef {
    public OpenQueueRef(long id, URI self, String key, String display, Session session) {
        super(id, self, key, display, session);
    }
}
