package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType;

public class TestModerationDiag {
    private TestModerationDiag() {
    }

    public static final long DIAG_ID1 = 1L;
    public static final long DIAG_ID2 = 33L;
    public static final long DIAG_ID3 = 133L;

    public static ModerationDiag createModerationDiag1() {
        return new ModerationDiag()
                .withId(DIAG_ID1)
                .withType(ModerationDiagType.COMMON)
                .withShortText("short text1")
                .withFullText("full text1")
                .withAllowFirstAid(true)
                .withStrongReason(true)
                .withUnbanIsProhibited(true)
                .withToken("token1");
    }

    public static ModerationDiag createModerationDiag2() {
        return new ModerationDiag()
                .withId(DIAG_ID2)
                .withType(ModerationDiagType.COMMON)
                .withShortText("short text2")
                .withFullText(null)
                .withAllowFirstAid(false)
                .withStrongReason(false)
                .withUnbanIsProhibited(false)
                .withToken("token2");
    }

    public static ModerationDiag createModerationDiagPerformance() {
        return new ModerationDiag()
                .withId(DIAG_ID3)
                .withType(ModerationDiagType.PERFORMANCE)
                .withShortText("short text3")
                .withFullText("full text3")
                .withAllowFirstAid(false)
                .withStrongReason(false)
                .withUnbanIsProhibited(false)
                .withToken("token3");
    }
}
