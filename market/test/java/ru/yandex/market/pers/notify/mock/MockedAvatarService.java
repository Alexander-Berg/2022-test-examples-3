package ru.yandex.market.pers.notify.mock;

import javax.annotation.Nullable;

import ru.yandex.market.pers.notify.avatar.AvatarService;

public class MockedAvatarService implements AvatarService {
    @Override
    public String makeUserAvatarUrl(@Nullable String avatarSlug) {
        return "http://market.yandex.ru/" + avatarSlug;
    }
}
