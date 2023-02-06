package ru.yandex.market.sre.services.tms.tasks.startrek.model;

import ru.yandex.startrek.client.model.UserRef;

public class UserRefExt extends UserRef {
    public UserRefExt(String id) {
        super(id, null, null, null);
    }
    public UserRefExt() { super(null, null, null, null); }
}
