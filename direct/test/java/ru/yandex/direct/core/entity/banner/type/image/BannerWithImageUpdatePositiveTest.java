package ru.yandex.direct.core.entity.banner.type.image;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithImageUpdatePositiveTest
        extends BannerNewBannerInfoUpdateOperationTestBase {

    @Autowired
    private TestModerationRepository moderationRepository;

    @Test
    public void changeBannerWithImageForImageBanner() {
        bannerInfo = steps.imageBannerSteps().createImageBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        var newImageHash = createImageAdImageFormat();

        var modelChanges = createImageBannerModelChanges(bannerId, newImageHash);

        Long id = prepareAndApplyValid(modelChanges);
        ImageBanner actualBanner = getBanner(id, ImageBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(newImageHash));
        assertThat(actualBanner.getImageStatusModerate(), equalTo(NewStatusImageModerate.READY));
    }

    @Test
    public void changeDraftBannerWithImageForImageBanner() {
        bannerInfo = steps.imageBannerSteps().createImageBannerWithImage();
        int shard = bannerInfo.getShard();
        Long bannerId = bannerInfo.getBannerId();
        addModerationData(shard, bannerId);

        String newImageHash = createImageAdImageFormat();

        var modelChanges = createImageBannerModelChanges(bannerId, newImageHash);

        Long id = prepareAndApplyValid(Collections.singletonList(modelChanges), ModerationMode.FORCE_SAVE_DRAFT).get(0);
        ImageBanner actualBanner = getBanner(id, ImageBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(newImageHash));
        assertThat(actualBanner.getImageStatusModerate(), equalTo(NewStatusImageModerate.NEW));

        Long modReasonImageIdId = moderationRepository.getModReasonImageIdByBannerId(shard, id);
        Long modObjectVersionId = moderationRepository.getModObjectVersionImageIdByBannerId(shard, id);
        assertThat(modReasonImageIdId, nullValue());
        assertThat(modObjectVersionId, notNullValue());
    }

    private ModelChanges<ImageBanner> createImageBannerModelChanges(Long bannerId, String newImageHash) {
        return new ModelChanges<>(bannerId, ImageBanner.class)
                .process(newImageHash, ImageBanner.IMAGE_HASH);
    }

    private String createImageAdImageFormat() {
        return steps.bannerSteps().createImageAdImageFormat(bannerInfo.getClientInfo())
                .getImageHash();
    }

    private void addModerationData(int shard, Long bannerId) {
        moderationRepository.addModReasonImage(shard, bannerId);
        moderationRepository.addModObjectVersionImage(shard, bannerId);
    }

}
