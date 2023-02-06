package ru.yandex.direct.bsexport.snapshot.holders;

import ru.yandex.direct.bsexport.snapshot.model.ExportedUser;

public class TestUsersHolder extends UsersHolder {
    public TestUsersHolder() {
        //noinspection ConstantConditions
        super(null, null);
    }

    @Override
    protected void checkInitialized() {
    }

    public void put(ExportedUser user) {
        Long uid = user.getId();
        put(uid, user);
    }
}
