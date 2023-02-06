package ru.yandex.direct.core.entity.image.service;

import org.apache.commons.lang.ArrayUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.image.container.UploadedToMdsImageInformation;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.image.repository.ImageDataRepository;
import ru.yandex.direct.core.entity.image.service.validation.ImageDefects;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageFromUrlToMdsValidationService;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageFromUrlValidationService;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageValidationService;
import ru.yandex.direct.core.entity.image.service.validation.type.ImageSaveValidationSupportFacade;
import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_TEXT_BANNER;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.getMockAvatarsClientPool;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ImageServiceSaveImageFromUrlToMdsTest {
    private ImageService imageService;

    @Autowired
    public ClientSteps clientSteps;

    @Autowired
    public FeatureService featureService;

    @Autowired
    private MdsFileService mdsFileService;

    @Autowired
    public BannerImagePoolRepository bannerImagePoolRepository;

    @Autowired
    public BannerImageFormatRepository bannerImageFormatRepository;

    @Autowired
    public ShardHelper shardHelper;

    @Autowired
    public ImageSaveValidationSupportFacade imageSaveValidationSupportFacade;

    @Autowired
    private SaveImageFromUrlToMdsValidationService saveImageFromUrlToMdsValidationService;

    @Autowired
    private SaveImageFromUrlValidationService saveImageFromUrlValidationService;

    @Autowired
    private BannerImageRepository bannerImageRepository;
    @Autowired
    private BannerCommonRepository bannerCommonRepository;
    @Autowired
    private ImageDataRepository imageDataRepository;

    @Autowired
    private SaveImageValidationService saveImageValidationService;
    @Autowired
    private AsyncHttpClient asyncHttpClient;
    private ClientInfo defaultClient;

    @Before
    public void before() {
        defaultClient = clientSteps.createDefaultClient();
    }

    private void imageServiceInit(byte[] imageData) {
        ImageDownloader imageDownloader = mock(ImageDownloader.class);
        when(imageDownloader.download(any())).thenReturn(imageData);
        imageService = new ImageService(imageSaveValidationSupportFacade, getMockAvatarsClientPool(),
                getMockAvatarsClientPool(), imageDownloader, asyncHttpClient, shardHelper,
                featureService, saveImageFromUrlToMdsValidationService, saveImageFromUrlValidationService, saveImageValidationService,
                mdsFileService,
                bannerImagePoolRepository, bannerImageFormatRepository, bannerImageRepository, bannerCommonRepository,
                imageDataRepository);
    }

    @Test
    public void saveImageFromUrlToMds_UrlValid() {
        imageServiceInit(generateBlankGifImageData(607, 1080));
        String name = "name.png";
        Result<UploadedToMdsImageInformation> uploadedToMdsFileInformationResult =
                imageService.saveImageFromUrlToMds(defaultClient.getClientId(), "https://ya.ru/" + name);
        UploadedToMdsImageInformation expectedUploadedTextBannerImageInformation = new UploadedToMdsImageInformation()
                .withName(name);
        assertThat(uploadedToMdsFileInformationResult.getErrors()).isEmpty();
        assertThat(uploadedToMdsFileInformationResult.getResult())
                .is(matchedBy(beanDiffer(expectedUploadedTextBannerImageInformation)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void saveImageFromUrlToMds_UrlValidImageTooBig() {
        byte[] imageData = generateBlankGifImageData(128, 128);
        byte[] trashData = new byte[MAX_IMAGE_FILE_SIZE_FOR_TEXT_BANNER];
        imageServiceInit(ArrayUtils.addAll(imageData, trashData));
        String name = "name.png";
        Result<UploadedToMdsImageInformation> uploadedToMdsFileInformationResult =
                imageService.saveImageFromUrlToMds(defaultClient.getClientId(), "https://ya.ru/" + name);
        assertThat(uploadedToMdsFileInformationResult.getErrors()).isNotEmpty();
        assertThat(uploadedToMdsFileInformationResult.getValidationResult())
                .is(matchedBy(
                        hasDefectDefinitionWith(validationError(path(), ImageDefects.imageFileSizeGreaterThanMax()))));
    }

    @Test
    public void saveImageFromUrlToMds_UrlInvalid() {
        imageServiceInit(generateBlankGifImageData(607, 1080));

        Result<UploadedToMdsImageInformation> uploadedToMdsFileInformationResult =
                imageService.saveImageFromUrlToMds(defaultClient.getClientId(), "123");
        assertThat(uploadedToMdsFileInformationResult.getErrors()).isNotEmpty();
    }
}
