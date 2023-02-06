package ru.yandex.direct.core.entity.banner.type.statusbssync;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithBannerImageUpdateBsSyncedTest extends BannerWithRelatedEntityUpdateBsSyncedTestBase<TextBanner> {


    @Override
    protected Long createPlainBanner() {
        return steps.bannerSteps().createBanner(activeTextBanner(), clientInfo).getBannerId();
    }

    @Override
    protected Long createBannerWithRelatedEntity() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), clientInfo);
        steps.bannerSteps().createBannerImage(bannerInfo);
        return bannerInfo.getBannerId();
    }


    @Override
    protected ModelChanges<TextBanner> getPlainModelChanges(Long bannerId) {
        return new ModelChanges<>(bannerId, TextBanner.class);
    }

    @Override
    protected void setNewRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        String imageHash = steps.bannerSteps().createRegularImageFormat(clientInfo).getImageHash();
        modelChanges.process(imageHash, TextBanner.IMAGE_HASH);
    }

    @Override
    protected void deleteRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        modelChanges.process(null, TextBanner.IMAGE_HASH);
    }
}
