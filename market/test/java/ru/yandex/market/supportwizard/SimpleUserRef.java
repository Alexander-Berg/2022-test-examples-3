package ru.yandex.market.supportwizard;

import java.net.URI;

import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.UserRef;

/**
 * Простая реализация для {@link UserRef}. Нужна, так как нет реализаций с public-конструкторами
 *
 * @author Vadim Lyalin
 */
public class SimpleUserRef extends UserRef {
    public SimpleUserRef(String id) {
        this(id, null, null, null);
    }

    public SimpleUserRef(String id, URI self, String display, Session session) {
        super(id, self, display, session);
    }
}
