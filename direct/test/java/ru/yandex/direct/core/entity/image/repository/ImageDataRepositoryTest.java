package ru.yandex.direct.core.entity.image.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.image.container.BannerImageType;
import ru.yandex.direct.core.entity.image.container.ImageFilterContainer;
import ru.yandex.direct.core.entity.image.model.AvatarHost;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace;
import ru.yandex.direct.core.entity.image.model.Image;
import ru.yandex.direct.core.entity.image.model.ImageFormat;
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta;
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta;
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.ALLOWED_SIZES_FOR_MCBANNER;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ImageDataRepositoryTest {
    @Autowired
    public BannerImagePoolRepository bannerImagePoolRepository;

    @Autowired
    public BannerImageFormatRepository bannerImageFormatRepository;

    @Autowired
    public BannerImageRepository bannerImageRepository;

    @Autowired
    public ImageDataRepository imageDataRepository;

    @Autowired
    public TestBannerImageRepository testBannerImageRepository;

    @Autowired
    public BannerSteps bannerSteps;

    @Autowired
    CampaignSteps campaignSteps;

    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    public ClientSteps clientSteps;
    private ClientInfo defaultClient;
    private String imageHash;

    @Before
    public void before() {
        defaultClient = clientSteps.createDefaultClient();
        imageHash = RandomStringUtils.randomAlphabetic(22);
        bannerSteps.addImageToImagePool(defaultClient.getShard(), defaultClient.getClientId(), imageHash);
        featureSteps.setCurrentClient(defaultClient.getClientId());
    }

    @Test
    public void getImages_ImageSizeIsAvailableForImageAdType_ImageReturns() {
        ImageSize size = ALLOWED_SIZES_FOR_MCBANNER.stream().findAny().get();
        addBannerImageFormat(size);

        ImageFilterContainer filter = new ImageFilterContainer().withBannerImageType(
                BannerImageType.BANNER_IMAGE_AD);
        List<Image> images = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(), filter);
        assertThat(images).hasSize(1);
        assertThat(images.get(0).getSize()).isEqualTo(size);
    }


    @Test
    public void getImages_ImageSizeIsNotAvailableForImageAdType_ImageNotReturns() {
        ImageSize notAvailableSize = new ImageSize()
                .withWidth(1)
                .withHeight(1);

        addBannerImageFormat(notAvailableSize);

        ImageFilterContainer filter = new ImageFilterContainer().withBannerImageType(
                BannerImageType.BANNER_IMAGE_AD);
        List<Image> images = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(),
                        filter);
        assertThat(images).isEmpty();
    }

    @Test
    public void getImages_filterByWidth_success() {
        ImageSize size = new ImageSize()
                .withWidth(1234)
                .withHeight(1234);

        addBannerImageFormat(size);

        ImageFilterContainer filter = new ImageFilterContainer().withWidth(size.getWidth());
        List<Image> images = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(),
                        filter);
        assertThat(images).hasSize(1);
        assertThat(images.get(0).getSize()).isEqualTo(size);
    }

    @Test
    public void getImages_filterByWidth_noneFits() {
        ImageSize size = new ImageSize()
                .withWidth(1234)
                .withHeight(1234);

        addBannerImageFormat(size);

        ImageFilterContainer filter = new ImageFilterContainer().withWidth(size.getWidth() + 1);
        List<Image> images = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(),
                        filter);
        assertThat(images).isEmpty();
    }

    @Test
    public void getImages_filterByHeight_success() {
        ImageSize size = new ImageSize()
                .withWidth(1234)
                .withHeight(1234);

        addBannerImageFormat(size);

        ImageFilterContainer filter = new ImageFilterContainer().withHeight(size.getHeight());
        List<Image> images = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(),
                        filter);
        assertThat(images).hasSize(1);
        assertThat(images.get(0).getSize()).isEqualTo(size);
    }

    @Test
    public void getImages_filterByHeight_noneFits() {
        ImageSize size = new ImageSize()
                .withWidth(1234)
                .withHeight(1234);

        addBannerImageFormat(size);

        ImageFilterContainer filter = new ImageFilterContainer().withHeight(size.getHeight() + 1);
        List<Image> images = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(),
                        filter);
        assertThat(images).isEmpty();
    }

    @Test
    public void overrideImageSmartCenters_success() {
        ImageSize size = ALLOWED_SIZES_FOR_MCBANNER.stream().findAny().get();
        addBannerImageFormat(size);

        ImageFilterContainer filter = new ImageFilterContainer().withBannerImageType(
                BannerImageType.BANNER_IMAGE_AD);
        List<Image> images = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(), filter);

        ImageSmartCenter newSmartCenter = new ImageSmartCenter().withX(1).withY(2);
        Map<String, ImageSmartCenter> smartCenters = ImmutableMap.of("1:1", newSmartCenter);
        Map<String, ImageSizeMeta> sizes = new HashMap<>();
        sizes.put("x150", new ImageSizeMeta()
                .withSmartCenters(smartCenters)
                .withHeight(1));
        ImageMdsMeta newMeta = new ImageMdsMeta().withSizes(sizes);

        imageDataRepository.overrideImageSmartCenters(defaultClient.getShard(), defaultClient.getClientId(),
                images.get(0).getImageHash(), JsonUtils.toJson(newMeta));

        List<Image> updatedImages = imageDataRepository
                .getImages(defaultClient.getShard(), defaultClient.getClientId(), filter);
        assertThat(updatedImages.get(0).getMdsMetaUserOverride()).isEqualTo(newMeta);
        assertThat(updatedImages.get(0).getMdsMeta()).isNotEqualTo(newMeta);
    }

    @Test
    public void getBidsByImageHash_success() {
        CampaignInfo defaultCampaign = campaignSteps.createActiveCampaign(defaultClient);
        TextBannerInfo firstTextBanner = bannerSteps.createActiveTextBanner(defaultCampaign);
        BannerImageInfo<TextBannerInfo> firstBannerImage = bannerSteps.createBannerImage(firstTextBanner);
        String imageHash = firstBannerImage.getBannerImageFormat().getImageHash();

        ClientInfo secondClient = clientSteps.createDefaultClient();
        CampaignInfo secondCampaign = campaignSteps.createActiveCampaign(secondClient);
        TextBannerInfo secondTextBanner = bannerSteps.createActiveTextBanner(secondCampaign);
        BannerImageInfo<TextBannerInfo> secondBannerImage = bannerSteps.createBannerImage(secondTextBanner);
        String imageHash1 = secondBannerImage.getBannerImageFormat().getImageHash();

        // Создаем изображение для второго баннера с хэшом как у изображения первого баннера
        bannerSteps.createBannerImage(secondTextBanner,
                secondBannerImage.getBannerImageFormat().withImageHash(imageHash),
                secondBannerImage.getBannerImage().withImageHash(imageHash));

        //noinspection unchecked
        Collection<Long> bidsByImageHash =
                bannerImageRepository.getBidsByImageHash(defaultClient.getShard(),
                        singletonList(imageHash),
                        defaultClient.getClientId().asLong());

        assertThat(bidsByImageHash.size()).isEqualTo(1);
        assertThat(bidsByImageHash.contains(firstTextBanner.getBannerId())).isTrue();
    }

    private void addBannerImageFormat(ImageSize size) {
        BannerImageFormat bannerImageFormat = new BannerImageFormat()
                .withImageHash(imageHash)
                .withFormats(Map.of("x450", new ImageFormat().withHeight(400).withWidth(450)))
                .withMdsMeta(JsonUtils.toJson(createDefaultMdsMeta()))
                .withImageType(ImageType.IMAGE_AD)
                .withAvatarsHost(AvatarHost.AVATARS_MDST_YANDEX_NET)
                .withMdsGroupId(1)
                .withNamespace(BannerImageFormatNamespace.DIRECT_PICTURE)
                .withSize(size);

        bannerImageFormatRepository
                .addBannerImageFormat(defaultClient.getShard(), singletonList(bannerImageFormat));
    }

    private ImageMdsMeta createDefaultMdsMeta() {
        ImageSmartCenter smartCenter = new ImageSmartCenter()
                .withHeight(1)
                .withWidth(1)
                .withX(0)
                .withY(0);
        ImageSizeMeta sizeMeta = new ImageSizeMeta()
                .withHeight(10)
                .withWidth(10)
                .withPath("path")
                .withSmartCenters(ImmutableMap.of("1:1", smartCenter));
        return new ImageMdsMeta()
                .withSizes(ImmutableMap.of(
                        "x150", sizeMeta,
                        "x300", sizeMeta));
    }

}
