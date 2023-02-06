package ru.yandex.direct.core.entity.banner.type.statusbssync;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCreativeUpdateBsSyncedTest extends BannerWithRelatedEntityUpdateBsSyncedTestBase<TextBanner> {

    @Override
    protected Long createPlainBanner() {
        return createBanner(null);
    }

    @Override
    protected Long createBannerWithRelatedEntity() {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo);
        return createBanner(creativeInfo.getCreativeId());
    }

    @Override
    protected ModelChanges<TextBanner> getPlainModelChanges(Long bannerId) {
        return new ModelChanges<>(bannerId, TextBanner.class);
    }

    @Override
    protected void setNewRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo);
        modelChanges.process(creativeInfo.getCreativeId(), CpmBanner.CREATIVE_ID);
    }

    @Override
    protected void deleteRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        modelChanges.process(null, TextBanner.CREATIVE_ID);
    }

    private Long createBanner(Long creativeId) {
        OldTextBanner textBanner = activeTextBanner().withCreativeId(creativeId);
        return steps.bannerSteps().createBanner(textBanner, clientInfo).getBannerId();
    }
}
