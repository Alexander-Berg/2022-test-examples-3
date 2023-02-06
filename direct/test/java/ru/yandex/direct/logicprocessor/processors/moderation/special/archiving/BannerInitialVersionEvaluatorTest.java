package ru.yandex.direct.logicprocessor.processors.moderation.special.archiving;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.creative.repository.CreativeInfo;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.ess.logicobjects.moderation.banner.BannerModerationEventsObject;
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration;
import ru.yandex.direct.logicprocessor.processors.moderation.banner.support.BannerModerationEventsWithInfo;

@ContextConfiguration(classes = EssLogicProcessorTestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class BannerInitialVersionEvaluatorTest {

    @Autowired
    private BannerInitialVersionEvaluator evaluator;

    @Test
    public void testNonExisting() {
        var event = new BannerModerationEventsObject("tag", 0L, 1L, 2L, 3L,
                BannersBannerType.mobile_content, false, false);
        var eventWithInfo = new BannerModerationEventsWithInfo("tag", 0L, false)
                .withObject(event)
                .withCampaignType(CampaignType.PERFORMANCE)
                .withHasImage(true);

        Long version = evaluator.getInitialVersion(eventWithInfo);
        Assertions.assertNull(version);
    }

    @Test
    public void testCpmCanvas() {
        var event = new BannerModerationEventsObject("tag", 0L, 1L, 2L, 3L,
                BannersBannerType.cpm_banner, false, false);
        var eventWithInfo = new BannerModerationEventsWithInfo("tag", 0L, false)
                .withObject(event)
                .withAdGroupType(AdGroupType.CPM_BANNER)
                .withCreativeInfo(new CreativeInfo(CreativeType.CANVAS, false, 100500L));

        Long version = evaluator.getInitialVersion(eventWithInfo);
        Assertions.assertEquals(5000L, version);
    }
}
