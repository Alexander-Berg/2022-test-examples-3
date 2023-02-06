package ru.yandex.direct.core.testing.steps;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.testing.info.ModerateBannerPageInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationReasonsRepository;

import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER_PAGE;

public class ModerationReasonSteps {

    private final TestModerationReasonsRepository testModerationReasonsRepository;
    private final BannerSteps bannerSteps;

    public ModerationReasonSteps(
            TestModerationReasonsRepository testModerationReasonsRepository,
            BannerSteps bannerSteps) {
        this.testModerationReasonsRepository = testModerationReasonsRepository;
        this.bannerSteps = bannerSteps;
    }

    public void clean() {
        testModerationReasonsRepository.clean();
    }

    public void insertStandartReasons() {
        testModerationReasonsRepository
                .insertReasons(TestModerationReasonsRepository.SHARD,
                        TestModerationReasonsRepository.STANDART_MODERATION_REASONS_BY_OBJECT_TYPE_AND_ID
                );
    }

    public void insertRejectReasonForBanner(OldBanner banner) {
        testModerationReasonsRepository.insertReasons(
                TestModerationReasonsRepository.SHARD,
                new EnumMap<ModerationReasonObjectType, List<Long>>(ModerationReasonObjectType.class) {{
                    put(BANNER, Collections.singletonList(banner.getId()));
                }});
    }

    public Long insertRejectedBannerAndReason() {
        TextBannerInfo banner = bannerSteps.createActiveTextBanner();
        insertRejectReasonForBanner(banner.getBanner());
        return banner.getBannerId();
    }

    public void insertRejectReasonForModerateBannerPage(ModerateBannerPageInfo moderateBannerPage) {
        testModerationReasonsRepository.insertReasons(
                moderateBannerPage.getShard(),
                new EnumMap<ModerationReasonObjectType, List<Long>>(ModerationReasonObjectType.class) {{
                    put(BANNER_PAGE, Collections.singletonList(moderateBannerPage.getModerateBannerPageId()));
                }});
    }

    public void insertRejectReasons(int shard, Map<ModerationReasonObjectType,
            List<Long>> moderationReasonsByObjectTypeAndId, List<ModerationReasonDetailed> reasons) {
        testModerationReasonsRepository.insertReasons(shard, moderationReasonsByObjectTypeAndId, reasons);
    }
}
