package ru.yandex.direct.core.entity.banner.type.bannerimage;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithModerationStatuses.STATUS_MODERATE;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithBannerImageUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithBannerImage> {


    @Autowired
    private TestModerationRepository moderationRepository;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private TestAdGroupRepository adGroupRepository;


    @Test
    public void validBannerWithImageForTextBanner() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();

        String newImageHash = createBannerImageWithRegularType();

        ModelChanges<TextBanner> modelChanges = createTextBannerModelChanges(bannerId, newImageHash);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(newImageHash));
        assertThat(actualBanner.getImageStatusModerate(), equalTo(StatusBannerImageModerate.READY));

    }

    @Test
    public void validBannerWithOutImageForTextBanner() {
        bannerInfo = createTextBannerWithoutBannerImage();
        Long bannerId = bannerInfo.getBannerId();

        String newImageHash = createBannerImageWithRegularType();

        ModelChanges<TextBanner> modelChanges = createTextBannerModelChanges(bannerId, newImageHash);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(newImageHash));
        assertThat(actualBanner.getImageStatusModerate(), equalTo(StatusBannerImageModerate.READY));

    }


    @Test
    public void deleteBannerImageForTextBanner() {
        bannerInfo = createTextBanner();
        int shard = bannerInfo.getShard();
        Long bannerId = bannerInfo.getBannerId();
        addModerationData(shard, bannerId);
        adGroupRepository.updateStatusBsSynced(shard, bannerInfo.getAdGroupId(), PhrasesStatusbssynced.Yes);

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(null, BannerWithBannerImage.IMAGE_HASH);


        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getImageHash(), nullValue());

        Long modReasonImageIdId = moderationRepository.getModReasonImageIdByBannerId(shard, id);
        Long modObjectVersionId = moderationRepository.getModObjectVersionImageIdByBannerId(shard, id);
        assertThat(modReasonImageIdId, nullValue());
        assertThat(modObjectVersionId, nullValue());

        var adGroupStatusBsSynced = adGroupRepository.getStatusBsSynced(shard,
                Collections.singletonList(bannerInfo.getAdGroupId())).get(bannerInfo.getAdGroupId());
        assertThat(adGroupStatusBsSynced, equalTo(PhrasesStatusbssynced.Yes));
    }

    @Test
    public void addBannerImageForTextBanner() {
        bannerInfo = bannerSteps.createBanner(activeTextBanner());
        Long bannerId = bannerInfo.getBannerId();
        int shard = bannerInfo.getShard();
        adGroupRepository.updateStatusBsSynced(shard, bannerInfo.getAdGroupId(), PhrasesStatusbssynced.Yes);

        String newImageHash = createBannerImageWithRegularType();

        ModelChanges<TextBanner> modelChanges = createTextBannerModelChanges(bannerId, newImageHash);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(newImageHash));
        assertThat(actualBanner.getImageStatusModerate(), equalTo(StatusBannerImageModerate.READY));

        var adGroupStatusBsSynced = adGroupRepository.getStatusBsSynced(shard,
                Collections.singletonList(bannerInfo.getAdGroupId())).get(bannerInfo.getAdGroupId());
        assertThat(adGroupStatusBsSynced, equalTo(PhrasesStatusbssynced.No));
    }

    @Test
    public void validDraftBannerWithImageForTextBanner() {
        bannerInfo = createTextBanner();
        int shard = bannerInfo.getShard();
        Long bannerId = bannerInfo.getBannerId();
        addModerationData(shard, bannerId);

        String newImageHash = createBannerImageWithRegularType();

        ModelChanges<TextBanner> modelChanges = createTextBannerModelChanges(bannerId, newImageHash)
                .process(BannerStatusModerate.NEW, STATUS_MODERATE);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(newImageHash));
        assertThat(actualBanner.getImageStatusModerate(), equalTo(StatusBannerImageModerate.NEW));

        Long modReasonImageIdId = moderationRepository.getModReasonImageIdByBannerId(shard, id);
        Long modObjectVersionId = moderationRepository.getModObjectVersionImageIdByBannerId(shard, id);
        assertThat(modReasonImageIdId, nullValue());
        assertThat(modObjectVersionId, notNullValue());
    }

    @Test
    public void removeAndRevertImageForTextBanner() {
        TextBannerInfo textBannerInfo = createTextBanner();
        Long bannerId = textBannerInfo.getBannerId();
        String imageHash = textBannerInfo.getBanner().getBannerImage().getImageHash();
        bannerInfo = textBannerInfo;

        // удаляем картинку
        ModelChanges<TextBanner> modelChanges1 = new ModelChanges<>(bannerId, TextBanner.class)
                .process(null, BannerWithBannerImage.IMAGE_HASH);

        prepareAndApplyValid(modelChanges1);
        TextBanner actualBanner1 = getBanner(bannerId, TextBanner.class);
        assertThat(actualBanner1.getImageHash(), nullValue());


        // возвращаем картинку
        ModelChanges<TextBanner> modelChanges2 = new ModelChanges<>(bannerId, TextBanner.class)
                .process(imageHash, BannerWithBannerImage.IMAGE_HASH);

        prepareAndApplyValid(modelChanges2);
        TextBanner actualBanner2 = getBanner(bannerId, TextBanner.class);
        assertThat(actualBanner2.getImageHash(), equalTo(imageHash));
        assertThat(actualBanner2.getImageStatusShow(), equalTo(true));
    }


    private ModelChanges<TextBanner> createTextBannerModelChanges(Long bannerId, String newImageHash) {
        return new ModelChanges<>(bannerId, TextBanner.class)
                .process(newImageHash, BannerWithBannerImage.IMAGE_HASH);
    }

    private TextBannerInfo createTextBannerWithoutBannerImage() {
        return bannerSteps.createBanner(activeTextBanner());
    }

    private TextBannerInfo createTextBanner() {
        TextBannerInfo textBannerInfo = bannerSteps.createBanner(activeTextBanner());
        bannerSteps.createBannerImage(textBannerInfo);
        return textBannerInfo;
    }

    private String createBannerImageWithRegularType() {
        return bannerSteps.createRegularImageFormat(bannerInfo.getClientInfo())
                .getImageHash();
    }

    private void addModerationData(int shard, Long bannerId) {
        moderationRepository.addModReasonImage(shard, bannerId);
        moderationRepository.addModObjectVersionImage(shard, bannerId);
    }

}
