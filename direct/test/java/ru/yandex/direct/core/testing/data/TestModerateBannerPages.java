package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateOperator;

public class TestModerateBannerPages {

    public static ModerateBannerPage defaultModerateBannerPage() {
        return new ModerateBannerPage()
                .withBannerId(1L)
                .withPageId(2L)
                .withVersion(3L)
                .withStatusModerate(StatusModerateBannerPage.SENT)
                .withStatusModerateOperator(StatusModerateOperator.NONE)
                .withIsRemoved(false)
                .withComment("moderation comment")
                .withCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }
}
