package ru.yandex.market.pers.notify.mock;

import ru.yandex.market.pers.notify.passport.FullPassportService;
import ru.yandex.market.pers.notify.passport.model.FullUserInfo;

public class MockedFullPassportService implements FullPassportService {
    @Override
    public FullUserInfo getFullUserInfo(long uid) {
        return new FullUserInfo(uid);
    }
}
