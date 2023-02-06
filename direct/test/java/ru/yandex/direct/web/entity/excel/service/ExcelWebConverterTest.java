package ru.yandex.direct.web.entity.excel.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.image.model.AvatarHost;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.web.entity.excel.model.internalad.ImageInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class ExcelWebConverterTest {

    private static Object[][] parametrizedTestDataForGetTemplateVariableImageHashes() {
        String correctHash = "Qn3Y6xE7tGVbpfXfJXZAjw";
        String invalidHash1 = "картинка1.jpg";
        String invalidHash2 = correctHash + "Новая";

        return new Object[][]{
                {List.of(correctHash), Set.of(correctHash)},
                {List.of(correctHash, invalidHash1), Set.of(correctHash)},
                {List.of(invalidHash2, correctHash, invalidHash1), Set.of(correctHash)},
                {List.of(invalidHash2, invalidHash1), Set.of()},
        };
    }

    @Test
    @TestCaseName("imageHashes = {0}, expectedImageHashes = {1}")
    @Parameters(method = "parametrizedTestDataForGetTemplateVariableImageHashes")
    public void checkGetTemplateVariableImageHashes(List<String> imageHashes, Set<String> expectedImageHashes) {
        List<TemplateVariable> templateVariables = mapList(imageHashes,
                hash -> new TemplateVariable().withInternalValue(hash));

        Set<String> templateVariableImageHashes = ExcelWebConverter.getTemplateVariableImageHashes(templateVariables);
        assertThat(templateVariableImageHashes)
                .isEqualTo(expectedImageHashes);
    }

    @Test
    public void checkGetImagesInfo() {
        String existingImageHash = RandomStringUtils.randomAlphanumeric(11);
        Long imageTemplateResourceId = RandomNumberUtils.nextPositiveLong();
        Long imageTemplateResourceId2 = imageTemplateResourceId + 1;
        String imageFileName = RandomStringUtils.randomAlphanumeric(7) + ".jpg";
        String imageUrl = "https://blabla.com/" + RandomStringUtils.randomAlphanumeric(7) + ".jpg";

        List<TemplateVariable> templateVariables = getTemplateVariables(imageTemplateResourceId, existingImageHash,
                imageTemplateResourceId2, imageFileName, imageUrl);
        var existingImageFormatsByHash = Map.of(existingImageHash, getBannerImageFormat(existingImageHash));
        Map<String, String> imageFileNameByHashes = Map.of(existingImageHash, RandomStringUtils.randomAlphanumeric(11));
        Set<Long> imageTemplateResourceIds = Set.of(imageTemplateResourceId, imageTemplateResourceId2);

        List<ImageInfo> imagesInfo = ExcelWebConverter.getImagesInfo(templateVariables,
                existingImageFormatsByHash, imageFileNameByHashes, imageTemplateResourceIds);

        List<ImageInfo> expectedImagesInfo = List.of(
                getExpectedImageInfo(existingImageFormatsByHash.get(existingImageHash),
                        imageFileNameByHashes.get(existingImageHash)),
                new ImageInfo()
                        .withNeedUpload(true)
                        .withFileName(imageFileName),
                new ImageInfo()
                        .withNeedUpload(true)
                        .withFileName("")
                        .withUrl(imageUrl)
        );
        assertThat(imagesInfo)
                .is(matchedBy(beanDiffer(expectedImagesInfo)));
    }

    private static List<TemplateVariable> getTemplateVariables(Long imageTemplateResourceId, String existingImageHash,
                                                               Long imageTemplateResourceId2, String imageFileName,
                                                               String imageUrl) {
        return List.of(
                // image templateVariable with null value
                new TemplateVariable()
                        .withTemplateResourceId(imageTemplateResourceId),

                // not image templateVariable
                new TemplateVariable()
                        .withTemplateResourceId(imageTemplateResourceId - 1),

                // image templateVariable with existing imageHash
                new TemplateVariable()
                        .withTemplateResourceId(imageTemplateResourceId)
                        .withInternalValue(existingImageHash),

                // image templateVariable which need upload file
                new TemplateVariable()
                        .withTemplateResourceId(imageTemplateResourceId2)
                        .withInternalValue(imageFileName),

                // image templateVariable which need upload file by url
                new TemplateVariable()
                        .withTemplateResourceId(imageTemplateResourceId2)
                        .withInternalValue(imageUrl)
        );
    }

    private static BannerImageFormat getBannerImageFormat(String existingImageHash) {
        return new BannerImageFormat()
                .withImageHash(existingImageHash)
                .withAvatarsHost(AvatarHost.AVATARS_MDST_YANDEX_NET)
                .withMdsGroupId(RandomNumberUtils.nextPositiveInteger())
                .withNamespace(BannerImageFormatNamespace.DIRECT_PICTURE)
                .withSize(new ImageSize()
                        .withHeight(RandomNumberUtils.nextPositiveInteger())
                        .withWidth(RandomNumberUtils.nextPositiveInteger()));
    }

    private static ImageInfo getExpectedImageInfo(BannerImageFormat bannerImageFormat, String fileName) {
        return new ImageInfo()
                .withNeedUpload(false)
                .withFileName(fileName)
                .withImageHash(bannerImageFormat.getImageHash())
                .withHeight(bannerImageFormat.getSize().getHeight())
                .withWidth(bannerImageFormat.getSize().getWidth())
                .withMdsGroupId(bannerImageFormat.getMdsGroupId().toString())
                .withNamespace(bannerImageFormat.getNamespace().name());
    }

}
