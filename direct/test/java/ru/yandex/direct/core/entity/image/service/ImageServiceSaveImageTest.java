package ru.yandex.direct.core.entity.image.service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.avatars.client.model.AvatarInfo;
import ru.yandex.direct.avatars.client.model.answer.ImageSize;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool;
import ru.yandex.direct.core.entity.image.container.BannerImageType;
import ru.yandex.direct.core.entity.image.container.UploadedBannerImageInformation;
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace;
import ru.yandex.direct.core.entity.image.model.ImageUploadContainer;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.image.repository.ImageDataRepository;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageFromUrlToMdsValidationService;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageFromUrlValidationService;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageValidationService;
import ru.yandex.direct.core.entity.image.service.validation.type.ImageSaveValidationSupportFacade;
import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.ORIG;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.GROUP_ID;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.NAMESPACE;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.getMockAvatarsClientPool;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.mockUpload;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ImageServiceSaveImageTest {
    private ImageService imageService;

    @Autowired
    public ClientSteps clientSteps;

    @Autowired
    public FeatureSteps featureSteps;

    @Autowired
    public FeatureService featureService;

    @Autowired
    private SaveImageFromUrlToMdsValidationService saveImageFromUrlToMdsValidationService;

    @Autowired
    private SaveImageFromUrlValidationService saveImageFromUrlValidationService;

    @Autowired
    private SaveImageValidationService saveImageValidationService;

    @Autowired
    private MdsFileService mdsFileService;

    @Autowired
    public ImageDownloader imageDownloader;

    @Autowired
    public BannerImagePoolRepository bannerImagePoolRepository;

    @Autowired
    public BannerImageFormatRepository bannerImageFormatRepository;

    @Autowired
    public ImageSaveValidationSupportFacade imageSaveValidationSupportFacade;

    @Autowired
    private BannerImageRepository bannerImageRepository;
    @Autowired
    private BannerCommonRepository bannerCommonRepository;
    @Autowired
    private ImageDataRepository imageDataRepository;

    @Autowired
    public ShardHelper shardHelper;

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    private ClientInfo defaultClient;
    private AvatarsClientPool avatarsClientPool;
    private BannerImageFormatNamespace namespace;

    @Before

    public void before() {
        defaultClient = clientSteps.createDefaultClient();
        avatarsClientPool = getMockAvatarsClientPool();
        namespace =
                BannerImageFormatNamespace.valueOf(avatarsClientPool.getDefaultClient().getConf().getUploadNamespace()
                        .toUpperCase());
        imageService = new ImageService(imageSaveValidationSupportFacade, avatarsClientPool,
                avatarsClientPool, imageDownloader, asyncHttpClient, shardHelper, featureService, saveImageFromUrlToMdsValidationService,
                saveImageFromUrlValidationService, saveImageValidationService, mdsFileService,
                bannerImagePoolRepository, bannerImageFormatRepository,
                bannerImageRepository, bannerCommonRepository, imageDataRepository);
        featureSteps.setCurrentClient(defaultClient.getClientId());
    }

    @Test
    public void saveImageForTextBanner_InvalidSmallImage_HasErrors() {
        String originalFilename = "name.jpg";
        MultipartFile multipartFile =
                new MockMultipartFile("name", originalFilename, null, generateBlankGifImageData(25, 25));
        Result<UploadedBannerImageInformation> uploadedTextBannerImageInformationResult =
                imageService.saveImages(defaultClient.getClientId(), Collections.singletonMap(0, new ImageUploadContainer(0, multipartFile, null)),
                        BannerImageType.BANNER_TEXT).get(0);

        assertThat(uploadedTextBannerImageInformationResult.getErrors()).isNotEmpty();
    }

    @Test
    public void saveImageForTextBanner_NoFormatsFromAvatars_HasErrors() {
        ImageSize defaultImageSizeOfImageAd = getDefaultImageSizeOfImageAd();
        MultipartFile multipartFile = new MockMultipartFile("name", "name.jpg", null,
                generateBlankGifImageData(defaultImageSizeOfImageAd.getWidth(), defaultImageSizeOfImageAd.getHeight()));

        when(avatarsClientPool.getDefaultClient().upload(any(), any()))
                .then(l -> {
                    String key = UUID.randomUUID().toString();
                    return new AvatarInfo(NAMESPACE, GROUP_ID, key, null, Map.of(ORIG, defaultImageSizeOfImageAd));
                });

        var errors = imageService.saveImages(defaultClient.getClientId(), Map.of(0, new ImageUploadContainer(0, multipartFile, null)),
                BannerImageType.BANNER_TEXT)
                .get(0)
                .getErrors();

        assertThat(errors).isNotEmpty();
    }

    @Test
    public void saveImageForTextBanner_ValidImage_HasNoErrors() {

        String originalFilename = "name.jpg";
        ImageSize defaultImageSizeOfImageAd = getDefaultImageSizeOfImageAd();
        MultipartFile multipartFile =
                new MockMultipartFile("name", originalFilename, null,
                        generateBlankGifImageData(defaultImageSizeOfImageAd.getWidth(),
                                defaultImageSizeOfImageAd.getHeight()));

        mockUpload(defaultImageSizeOfImageAd, avatarsClientPool);

        Result<UploadedBannerImageInformation> uploadedTextBannerImageInformationResult =
                imageService.saveImages(this.defaultClient.getClientId(), Collections.singletonMap(0, new ImageUploadContainer(0, multipartFile, null)),
                        BannerImageType.BANNER_TEXT).get(0);


        UploadedBannerImageInformation expectedUploadedTextBannerImageInformation =
                new UploadedBannerImageInformation()
                        .withMdsGroupId(GROUP_ID)
                        .withName(originalFilename)
                        .withNamespace(namespace)
                        .withWidth(defaultImageSizeOfImageAd.getWidth())
                        .withHeight(defaultImageSizeOfImageAd.getHeight());

        assertThat(uploadedTextBannerImageInformationResult.getErrors()).isEmpty();
        assertThat(uploadedTextBannerImageInformationResult.getResult())
                .is(matchedBy(beanDiffer(expectedUploadedTextBannerImageInformation)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void saveImagesForImageBanner_ValidImages_HasNoErrors() {
        String originalFilename = "name.jpg";
        MultipartFile multipartFile1 =
                new MockMultipartFile("name", originalFilename, null, generateBlankGifImageData(640, 100));

        MultipartFile multipartFile2 =
                new MockMultipartFile("name", originalFilename, null, generateBlankGifImageData(640, 200));

        Map<Integer, ImageUploadContainer> imageFileByIndexFromReceived = ImmutableMap.<Integer, ImageUploadContainer>builder()
                .put(3, new ImageUploadContainer(3, multipartFile1, null))
                .put(5, new ImageUploadContainer(5, multipartFile2, null))
                .build();

        MassResult<UploadedBannerImageInformation> uploadedTextBannerImageInformationResult =
                imageService.saveImages(defaultClient.getClientId(), imageFileByIndexFromReceived,
                        BannerImageType.BANNER_IMAGE_AD);

        assertThat(uploadedTextBannerImageInformationResult.getErrors()).isEmpty();
    }

    @Test
    public void saveImagesForImageBanner_OneImageInvalid_OneImageHasNoErrorsOneImageHasErrors() {
        String originalFilename = "name.jpg";
        ImageSize defaultImageSizeOfImageAd = getDefaultImageSizeOfImageAd();

        MultipartFile multipartFile1 =
                new MockMultipartFile("name", originalFilename, null,
                        generateBlankGifImageData(defaultImageSizeOfImageAd.getWidth(),
                                defaultImageSizeOfImageAd.getHeight()));

        mockUpload(defaultImageSizeOfImageAd, avatarsClientPool);

        MultipartFile multipartFile2 =
                new MockMultipartFile("name", originalFilename, null, generateBlankGifImageData(1, 1));

        Map<Integer, ImageUploadContainer> imageFileByIndexFromReceived = ImmutableMap.<Integer, ImageUploadContainer>builder()
                .put(0, new ImageUploadContainer(0, multipartFile1, null))
                .put(10, new ImageUploadContainer(10, multipartFile2, null))
                .build();

        MassResult<UploadedBannerImageInformation> uploadedTextBannerImageInformationResult =
                imageService.saveImages(defaultClient.getClientId(), imageFileByIndexFromReceived,
                        BannerImageType.BANNER_TEXT);

        UploadedBannerImageInformation expectedUploadedTextBannerImageInformation =
                new UploadedBannerImageInformation()
                        .withMdsGroupId(GROUP_ID)
                        .withName(originalFilename)
                        .withNamespace(namespace);

        assertThat(uploadedTextBannerImageInformationResult.get(1).getErrors()).isNotEmpty();
        assertThat(uploadedTextBannerImageInformationResult.get(0).getErrors()).isEmpty();
        assertThat(uploadedTextBannerImageInformationResult.get(0).getResult())
                .is(matchedBy(beanDiffer(expectedUploadedTextBannerImageInformation)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }


    private static ImageSize getDefaultImageSizeOfImageAd() {
        int width = 660;
        int height = 1028;
        return new ImageSize().withWidth(width).withHeight(height);
    }
}
