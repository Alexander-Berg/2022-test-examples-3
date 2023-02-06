package ru.yandex.direct.core.entity.image.service.validation;

import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool;
import ru.yandex.direct.core.entity.image.container.BannerImageType;
import ru.yandex.direct.core.entity.image.container.UploadedBannerImageInformation;
import ru.yandex.direct.core.entity.image.model.BannerImageSource;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.image.repository.ImageDataRepository;
import ru.yandex.direct.core.entity.image.service.ImageDownloader;
import ru.yandex.direct.core.entity.image.service.ImageService;
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
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.getMockAvatarsClientPool;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ImageServiceSaveImageFromUrlTest {
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
    private MdsFileService mdsFileService;

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
    private SaveImageValidationService saveImageValidationService;
    @Autowired
    public ShardHelper shardHelper;
    @Autowired
    private AsyncHttpClient asyncHttpClient;

    private ClientInfo defaultClient;

    private ImageDownloader imageDownloader;

    @Before
    public void before() {
        imageDownloader = mock(ImageDownloader.class);
        defaultClient = clientSteps.createDefaultClient();
        AvatarsClientPool avatarsClientPool = getMockAvatarsClientPool();
        imageService =
                new ImageService(imageSaveValidationSupportFacade, avatarsClientPool, avatarsClientPool,
                        imageDownloader, asyncHttpClient, shardHelper, featureService, saveImageFromUrlToMdsValidationService,
                        saveImageFromUrlValidationService, saveImageValidationService, mdsFileService,
                        bannerImagePoolRepository, bannerImageFormatRepository,
                        bannerImageRepository, bannerCommonRepository, imageDataRepository);
    }

    @Test
    public void saveImageForTextBanner_InvalidSmallImage_HasErrors() {
        when(imageDownloader.download(any())).thenReturn(generateBlankGifImageData(25, 25));
        String originalFilename = "name.jpg";

        Result<UploadedBannerImageInformation> uploadedTextBannerImageInformationResult =
                imageService.saveImageFromUrl(defaultClient.getClientId(), "ya.ru/" + originalFilename,
                        BannerImageType.BANNER_TEXT, BannerImageSource.DIRECT, null);

        assertThat(uploadedTextBannerImageInformationResult.getErrors()).isNotEmpty();
    }

}
