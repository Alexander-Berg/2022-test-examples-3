package ru.yandex.direct.web.entity.banner.service;

import java.util.List;
import java.util.Set;

import junitparams.converters.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.direct.avatars.client.model.answer.ImageSize;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.image.repository.ImageDataRepository;
import ru.yandex.direct.core.entity.image.service.ImageDownloader;
import ru.yandex.direct.core.entity.image.service.ImageService;
import ru.yandex.direct.core.entity.image.service.validation.ImageDefectIds;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageFromUrlToMdsValidationService;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageFromUrlValidationService;
import ru.yandex.direct.core.entity.image.service.validation.SaveImageValidationService;
import ru.yandex.direct.core.entity.image.service.validation.type.ImageSaveValidationSupportFacade;
import ru.yandex.direct.core.entity.internalads.service.TemplateInfoService;
import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.entity.banner.service.validation.BannerImageValidationService;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.banner.model.BannerImagesUploadResponse;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.direct.core.entity.image.container.ImageFileFormat.GIF;
import static ru.yandex.direct.core.entity.image.container.ImageFileFormat.JPEG;
import static ru.yandex.direct.core.entity.image.container.ImageFileFormat.PNG;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_TEXT_IMAGE_BANNER;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankSvgImageDate;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.getMockAvatarsClientPool;
import static ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils.mockUpload;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerImageWebServiceTest {
    @Autowired
    public DirectWebAuthenticationSource authenticationSource;

    @Autowired
    public BannerImageValidationService bannerImageValidationService;

    @Autowired
    public FeatureService featureService;

    @Autowired
    public ValidationResultConversionService validationResultConversionService;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private SaveImageFromUrlToMdsValidationService saveImageFromUrlToMdsValidationService;

    @Autowired
    private SaveImageFromUrlValidationService saveImageFromUrlValidationService;

    @Autowired
    private SaveImageValidationService saveImageValidationService;

    @Autowired
    private MdsFileService mdsFileService;

    @Autowired
    public BannerImagePoolRepository bannerImagePoolRepository;

    @Autowired
    public BannerImageFormatRepository bannerImageFormatRepository;

    @Autowired
    public ImageSaveValidationSupportFacade imageSaveValidationSupportFacade;

    @Autowired
    public ShardHelper shardHelper;

    @Autowired
    private ImageDownloader imageDownloader;

    @Autowired
    private BannerImageRepository bannerImageRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private ImageDataRepository imageDataRepository;

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    @Autowired
    private TemplateInfoService templateInfoService;

    public BannerImageWebService bannerImageWebService;

    private AvatarsClientPool avatarsClientPool;

    @Before
    public void before() {
        testAuthHelper.createDefaultUser();

        avatarsClientPool = getMockAvatarsClientPool();
        ImageService imageService =
                new ImageService(imageSaveValidationSupportFacade, avatarsClientPool, avatarsClientPool,
                        imageDownloader, asyncHttpClient, shardHelper, featureService, saveImageFromUrlToMdsValidationService,
                        saveImageFromUrlValidationService, saveImageValidationService, mdsFileService,
                        bannerImagePoolRepository,
                        bannerImageFormatRepository, bannerImageRepository, bannerCommonRepository,
                        imageDataRepository);
        bannerImageWebService = new BannerImageWebService(authenticationSource, bannerImageValidationService,
                validationResultConversionService, imageService, templateInfoService);
    }

    @Test
    public void saveImagesForImageAd_OneImageInvalidForWebValidationOneImageInvalidForCoreValidation() {
        byte[] trashData = new byte[MAX_IMAGE_FILE_SIZE_FOR_TEXT_IMAGE_BANNER + 10];
        int validImageAdImageWidth = 300;
        int validImageAdImageHeight = 600;
        byte[] imageWithTooGreatFileSize = (ArrayUtils
                .addAll(generateBlankGifImageData(validImageAdImageWidth, validImageAdImageHeight), trashData));
        MultipartFile multipartFileInvalidForWeb =
                new MockMultipartFile("name1", "asdasd1.jpg", null, imageWithTooGreatFileSize);
        MultipartFile multipartFileInvalidForCore =
                new MockMultipartFile("name2", "asdasd2.jpg", null, generateBlankGifImageData(25, 25));
        MultipartFile multipartFileValid =
                new MockMultipartFile("name3", "asdasd3.jpg", null, generateBlankGifImageData(validImageAdImageWidth,
                        validImageAdImageHeight));

        mockUpload(new ImageSize().withWidth(validImageAdImageWidth).withHeight(validImageAdImageHeight),
                avatarsClientPool);

        WebResponse webResponse = bannerImageWebService
                .saveImages(asList(multipartFileInvalidForWeb, multipartFileInvalidForCore, multipartFileValid),
                        "BANNER_IMAGE_AD");

        assertThat(webResponse).isOfAnyClassIn(BannerImagesUploadResponse.class);

        BannerImagesUploadResponse bannerImagesUploadResponse = (BannerImagesUploadResponse) webResponse;
        assertThat(bannerImagesUploadResponse.getResult()).hasSize(1);
        assertThat(bannerImagesUploadResponse.getResult().get(0).getName())
                .isEqualTo(multipartFileValid.getOriginalFilename());
        assertThat(bannerImagesUploadResponse.validationResult().getErrors()).hasSize(2);
    }

    @Test
    public void saveImagesForImageAd_SvgFormatValidForInternal() {
        int validImageAdImageWidth = 300;
        int validImageAdImageHeight = 600;
        MultipartFile imageAsMultipartFile =
                new MockMultipartFile("name3", "asdasd1.svg", null, generateBlankSvgImageDate(validImageAdImageWidth,
                        validImageAdImageHeight));

        mockUpload(new ImageSize().withWidth(validImageAdImageWidth).withHeight(validImageAdImageHeight),
                avatarsClientPool);

        WebResponse webResponse = bannerImageWebService
                .saveImages(List.of(imageAsMultipartFile), "BANNER_INTERNAL");

        assertThat(webResponse).isOfAnyClassIn(BannerImagesUploadResponse.class);

        BannerImagesUploadResponse bannerImagesUploadResponse = (BannerImagesUploadResponse) webResponse;
        assertThat(bannerImagesUploadResponse.validationResult().getErrors()).isEmpty();
        assertThat(bannerImagesUploadResponse.getResult()).extracting("name")
                .containsExactly(imageAsMultipartFile.getOriginalFilename());
    }

    @Test
    public void saveImagesForImageAd_SvgFormatInvalidForImageAd() {
        int validImageAdImageWidth = 300;
        int validImageAdImageHeight = 600;
        MultipartFile imageAsMultipartFile =
                new MockMultipartFile("name3", "asdasd1.svg", null, generateBlankSvgImageDate(validImageAdImageWidth,
                        validImageAdImageHeight));

        mockUpload(new ImageSize().withWidth(validImageAdImageWidth).withHeight(validImageAdImageHeight),
                avatarsClientPool);

        WebResponse webResponse = bannerImageWebService
                .saveImages(List.of(imageAsMultipartFile), "BANNER_IMAGE_AD");

        assertThat(webResponse).isOfAnyClassIn(ValidationResponse.class);

        ValidationResponse response = (ValidationResponse) webResponse;

        assertThat(response.validationResult().getErrors()).is(matchedBy(contains(
                webDefect("[0]", "ImageDefectIds.Format.IMAGE_FILE_FORMAT_NOT_ALLOWED",
                        new ImageDefectIds.ImageFileFormatParam(Set.of(PNG, GIF, JPEG)))
        )));
    }

    private Matcher<WebDefect> webDefect(String path, String defectCode, @Nullable Object params) {
        return allOf(
                instanceOf(WebDefect.class),
                hasProperty("path", equalTo(path)),
                hasProperty("code", equalTo(defectCode)),
                hasProperty("params", equalTo(params))
        );
    }
}
