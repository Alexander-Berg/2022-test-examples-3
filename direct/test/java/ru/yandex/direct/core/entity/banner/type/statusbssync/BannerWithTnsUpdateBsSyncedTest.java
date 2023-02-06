package ru.yandex.direct.core.entity.banner.type.statusbssync;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTnsUpdateBsSyncedTest extends BannerWithRelatedEntityUpdateBsSyncedTestBase<CpmBanner> {

    @Override
    protected Long createPlainBanner() {
        return createBanner(null);
    }

    @Override
    protected Long createBannerWithRelatedEntity() {
        return createBanner("oldTns");
    }

    @Override
    protected ModelChanges<CpmBanner> getPlainModelChanges(Long bannerId) {
        return new ModelChanges<>(bannerId, CpmBanner.class);
    }

    @Override
    protected void setNewRelatedEntity(ModelChanges<CpmBanner> modelChanges) {
        modelChanges.process("newTns", CpmBanner.TNS_ID);
    }

    @Override
    protected void deleteRelatedEntity(ModelChanges<CpmBanner> modelChanges) {
        modelChanges.process(null, CpmBanner.TNS_ID);
    }

    private Long createBanner(String tnsId) {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        OldCpmBanner banner = activeCpmBanner(null, null, creativeInfo.getCreativeId()).withTnsId(tnsId);
        return steps.bannerSteps().createActiveCpmBanner(banner, clientInfo).getBannerId();
    }
}
