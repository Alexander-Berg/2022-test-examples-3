package ru.yandex.direct.web.entity.banner.service;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.direct.core.entity.image.container.BannerImageType;
import ru.yandex.direct.core.entity.image.model.ImageUploadContainer;
import ru.yandex.direct.core.entity.image.service.validation.ImageDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.core.entity.banner.service.validation.BannerImageValidationService;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_TEXT_IMAGE_BANNER;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

public class BannerImageValidationServiceTest {
    public BannerImageValidationService bannerImageValidationService;

    @Before
    public void before() {
        bannerImageValidationService = new BannerImageValidationService();
    }

    @Test
    public void validateImages_InvalidFileSize_HasError() {
        MultipartFile multipartFileWithTooGreatSize = getMultipartFileWithTooGreatSize();
        ValidationResult<List<Integer>, Defect> vr = bannerImageValidationService
                .validateImages(Collections.singletonList(0),
                        Collections.singletonMap(0, new ImageUploadContainer(0, multipartFileWithTooGreatSize, null)),
                        BannerImageType.BANNER_IMAGE_AD, false);

        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(ImageDefectIds.Gen.IMAGE_FILE_SIZE_GREATER_THAN_MAX)));
    }

    private MultipartFile getMultipartFileWithTooGreatSize() {
        byte[] trashData = new byte[MAX_IMAGE_FILE_SIZE_FOR_TEXT_IMAGE_BANNER];
        byte[] imageWithTooGreatFileSize = (ArrayUtils.addAll(generateBlankGifImageData(25, 25), trashData));
        return new MockMultipartFile("name", "asdasd1.jpg", null, imageWithTooGreatFileSize);
    }

    @Test
    public void validateImages_ValidFile_HasNoErrors() {
        MultipartFile multipartFileInvalidForWeb =
                new MockMultipartFile("name", "asdasd1.jpg", null, generateBlankGifImageData(25, 25));
        ValidationResult<List<Integer>, Defect> vr = bannerImageValidationService
                .validateImages(Collections.singletonList(0),
                        Collections.singletonMap(0, new ImageUploadContainer(0, multipartFileInvalidForWeb, null)),
                        BannerImageType.BANNER_IMAGE_AD, false);

        assertThat("ошибки отстутвуют", vr, hasNoDefectsDefinitions());
    }


}
