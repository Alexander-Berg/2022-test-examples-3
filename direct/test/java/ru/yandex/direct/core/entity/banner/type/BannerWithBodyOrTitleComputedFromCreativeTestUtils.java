package ru.yandex.direct.core.entity.banner.type;

import ru.yandex.direct.core.entity.creative.model.ModerationInfoText;
import ru.yandex.direct.core.entity.creative.service.CreativeUtils;

public class BannerWithBodyOrTitleComputedFromCreativeTestUtils {

    public static final String TITLE = "some title 1";
    public static final ModerationInfoText TITLE_TEXT = new ModerationInfoText()
            .withText(TITLE)
            .withType(CreativeUtils.MODERATION_PROPERTY_TITLE);

    public static final String BODY = "some body 1";
    public static final ModerationInfoText BODY_TEXT = new ModerationInfoText()
            .withText(BODY)
            .withType(CreativeUtils.MODERATION_PROPERTY_BODY);

}
