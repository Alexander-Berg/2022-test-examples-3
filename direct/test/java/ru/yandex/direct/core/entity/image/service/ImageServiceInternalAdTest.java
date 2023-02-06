package ru.yandex.direct.core.entity.image.service;

import java.util.List;
import java.util.Map;

import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.avatars.client.model.answer.ImageSize;
import ru.yandex.direct.core.entity.banner.model.ImageType;
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
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo;
import ru.yandex.direct.core.entity.internalads.restriction.InternalAdRestrictionDefects;
import ru.yandex.direct.core.entity.internalads.restriction.Restrictions;
import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.GROUP_ID;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.getMockAvatarsClientPool;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.mockUpload;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ImageServiceInternalAdTest {

    private ImageService imageService;

    @Autowired
    public ClientSteps clientSteps;

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
    }


    @Test
    public void shouldSaveImagesOk() {
        var originalFilename = "fileName";
        var file1 = new MockMultipartFile("name", originalFilename, null, generateBlankGifImageData(640, 100));
        var file2 = new MockMultipartFile("name", originalFilename, null, generateBlankGifImageData(640, 100));
        var resourceInfo = new ResourceInfo()
                .withValueRestrictions(List.of(Restrictions.imageDimensionsMax(true, 1000, 1000)));
        var containers = Map.of(
            1, new ImageUploadContainer(1, file1, resourceInfo),
            2, new ImageUploadContainer(2, file2, resourceInfo)
        );

        var expectedUploadedBannerImageInformation = createUploadedBannerImageInformation(originalFilename);

        mockUpload(new ImageSize().withWidth(640).withHeight(100), avatarsClientPool);

        var result = imageService.saveImages(defaultClient.getClientId(), containers,BannerImageType.BANNER_INTERNAL);

        assertThat(result.getResult())
                .isNotNull()
                .hasSize(2);
        assertThat(result.get(0).getErrors())
                .isEmpty();
        assertThat(result.get(1).getErrors())
                .isEmpty();
        assertThat(result.get(0).getResult())
                .is(matchedBy(
                        beanDiffer(expectedUploadedBannerImageInformation)
                                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                        )
                );
        assertThat(result.get(1).getResult())
                .is(matchedBy(
                        beanDiffer(expectedUploadedBannerImageInformation)
                                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                        )
                );
    }

    @Test
    public void shouldReturnErrors_OneResultOk() {
        var fileName1 = "success";
        var fileName2 = "invaild file";
        var file1 = new MockMultipartFile("name", fileName1, null, generateBlankGifImageData(640, 100));
        var file2 = new MockMultipartFile("name", fileName2, null, generateBlankGifImageData(640, 100));
        var resourceInfo1 = new ResourceInfo()
                .withValueRestrictions(List.of(Restrictions.imageDimensionsMax(true, 1000, 1000)));
        var resourceInfo2 = new ResourceInfo()
                .withValueRestrictions(List.of(Restrictions.imageDimensionsMax(true, 100, 100)));
        var containers = Map.of(
                1, new ImageUploadContainer(1, file1, resourceInfo1),
                2, new ImageUploadContainer(2, file2, resourceInfo2)
        );

        var expectedUploadedBannerImageInformation = createUploadedBannerImageInformation(fileName1);

        mockUpload(new ImageSize().withWidth(640).withHeight(100), avatarsClientPool);

        var result = imageService.saveImages(defaultClient.getClientId(), containers,BannerImageType.BANNER_INTERNAL);

        assertThat(result.getResult())
                .isNotNull()
                .hasSize(2);
        assertThat(result.get(1).getValidationResult())
                .is(matchedBy(
                        hasDefectDefinitionWith(
                                validationError(emptyPath(), InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_BIG)
                        )
                ));
        assertThat(result.get(0).getErrors())
                .isEmpty();
        assertThat(result.get(0).getResult())
                .is(matchedBy(
                        beanDiffer(expectedUploadedBannerImageInformation)
                                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                        )
                );
    }

    private UploadedBannerImageInformation createUploadedBannerImageInformation(String fileName) {
        return new UploadedBannerImageInformation()
                .withMdsGroupId(GROUP_ID)
                .withName(fileName)
                .withHeight(640)
                .withHeight(100)
                .withImageType(ImageType.IMAGE_AD)
                .withNamespace(namespace);
    }
}
