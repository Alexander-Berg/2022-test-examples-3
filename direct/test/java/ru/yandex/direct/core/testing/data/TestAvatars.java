package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.avatars.client.model.AvatarId;

public class TestAvatars {
    private TestAvatars() {
    }

    public static AvatarId defaultAvatarId(long clientAvatarId) {
        return new AvatarId("direct-avatars", 10, "fake_image_name" + clientAvatarId);
    }

    public static String defaultAvatarExternalId(long clientAvatarId) {
        return defaultAvatarId(clientAvatarId).toAvatarIdString();
    }
}
