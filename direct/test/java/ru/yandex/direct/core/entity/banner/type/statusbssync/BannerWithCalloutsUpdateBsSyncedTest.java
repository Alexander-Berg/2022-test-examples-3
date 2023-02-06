package ru.yandex.direct.core.entity.banner.type.statusbssync;

import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithCalloutsUpdateBsSyncedTest extends BannerWithRelatedEntityUpdateBsSyncedTestBase<TextBanner> {

    @Override
    protected Long createPlainBanner() {
        return steps.bannerSteps().createBanner(activeTextBanner(), clientInfo).getBannerId();
    }

    @Override
    protected Long createBannerWithRelatedEntity() {
        List<Long> callouts = List.of(steps.calloutSteps().createDefaultCallout(clientInfo).getId());
        OldTextBanner banner = activeTextBanner().withCalloutIds(callouts);
        return steps.bannerSteps().createBanner(banner, clientInfo).getBannerId();
    }

    @Override
    protected ModelChanges<TextBanner> getPlainModelChanges(Long bannerId) {
        return new ModelChanges<>(bannerId, TextBanner.class);
    }

    @Override
    protected void setNewRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        List<Long> callouts = List.of(steps.calloutSteps().createDefaultCallout(clientInfo).getId());
        modelChanges.process(callouts, BannerWithCallouts.CALLOUT_IDS);
    }

    @Override
    protected void deleteRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        modelChanges.process(null, BannerWithCallouts.CALLOUT_IDS);
    }
}
