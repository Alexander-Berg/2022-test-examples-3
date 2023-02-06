package ru.yandex.direct.bsexport.testing.data;

import ru.yandex.direct.bsexport.model.Context;

/**
 * Контексты
 */
public class TestContext {
    private TestContext() {
    }

    public static final Context baseWithUpdateInfo1 = Context.newBuilder()
            .setEID(3580015259L)
            .setID(218699565L)
            .setUpdateInfo(1)
            .build();

    public static final Context baseWithUpdateInfo2 = Context.newBuilder()
            .setEID(3580015260L)
            .setID(218699565L)
            .setUpdateInfo(1)
            .build();

    public static final Context cpmBannerWithUpdateInfo1 = Context.newBuilder()
            .setEID(3590529107L)
            .setID(1808523229L)
            .setUpdateInfo(1)
            .build();

    public static final Context cpmBannerWithUpdateInfo2 = Context.newBuilder()
            .setEID(3590529108L)
            .setID(1808523229L)
            .setUpdateInfo(1)
            .build();
}
