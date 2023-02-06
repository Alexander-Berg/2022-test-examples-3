package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.service.ImageConstants;
import ru.yandex.direct.core.entity.image.service.validation.ImageDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.ALLOWED_SIZES_FOR_AD_IMAGE_ORIGINAL;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGES_PER_REQUEST_FOR_IMAGE_AD;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@CoreTest
public class ImageSaveValidationForTextImageAdSupportTest extends ImageSaveValidationSupportBaseTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private final FeatureService featureService;
    private final PpcPropertiesSupport ppcPropertiesSupport;
    private ImageSaveValidationForTextImageAdSupport saveValidationForTextImageAdSupport;

    @Autowired
    FeatureSteps featureSteps;

    @Autowired
    ClientSteps clientSteps;

    public ImageSaveValidationForTextImageAdSupportTest() {
        featureService = mock(FeatureService.class);
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        saveValidationForTextImageAdSupport = new ImageSaveValidationForTextImageAdSupport(
                featureService,
                ppcPropertiesSupport
        );
    }

    public static Collection<Object[]> getProportionallyLargerImages() {
        return StreamEx.of(ALLOWED_SIZES_FOR_AD_IMAGE_ORIGINAL)
                .map(size -> {
                            var randomCoefficient = 3;
                            var imageSize = new ImageSize()
                                    .withHeight(size.getHeight() * randomCoefficient)
                                    .withWidth(size.getWidth() * randomCoefficient);
                            return new Object[]{
                                    String.format(
                                            "test for image with size %sx%s",
                                            imageSize.getWidth(),
                                            imageSize.getHeight()
                                    ),
                                    imageSize
                            };
                        }
                )
                .toList();
    }

    @Before
    public void prepare() {
        var client = clientSteps.createDefaultClient();
        featureSteps.setCurrentClient(client.getClientId());
    }

    @Test
    @Parameters(method = "getProportionallyLargerImages")
    @TestCaseName("{0} feature enabled")
    public void validate_ImageSizeProportionallyLargerWithFeature_HasNoError(String description,
                                                                             ImageSize imageSize) {
        featureSteps.enableClientFeature(FeatureName.ALLOW_PROPORTIONALLY_LARGER_IMAGES);
        var ppcProperty = Mockito.mock(PpcProperty.class);

        when(ppcProperty.getOrDefault(any(Integer.class)))
                .then(returnsFirstArg());
        when(ppcPropertiesSupport.get(any(), any()))
                .thenReturn(ppcProperty);
        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(
                        Collections.singletonList(getValidImageMetaInformation().withSize(imageSize)),
                        vr,
                        indexMap
                );
        assertThat(actualVr)
                .is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Parameters(method = "getProportionallyLargerImages")
    @TestCaseName("{0} feature disabled")
    public void validate_ImageSizeProportionallyLargerWithFeature_HasError(String description, ImageSize imageSize) {
        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(
                        Collections.singletonList(getValidImageMetaInformation().withSize(imageSize)),
                        vr,
                        indexMap
                );

        assertThat(actualVr)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0)),
                                        ImageDefects.imageSizeIsNotAllowed()))));

    }

    @Test
    @Parameters(method = "getProportionallyLargerImages")
    @TestCaseName("{0} feature enabled")
    public void validate_ImageSizeProportionallyLarger(String description, ImageSize imageSize) {
        var ppcProperty = Mockito.mock(PpcProperty.class);

        when(ppcProperty.getOrDefault(any(Integer.class))).then(returnsFirstArg());
        when(ppcPropertiesSupport.get(any(), any()))
                .thenReturn(ppcProperty);
        var proportionallyLarger = saveValidationForTextImageAdSupport.isProportionallyLarger(imageSize);

        assertThat(proportionallyLarger).isTrue();
    }

    @Override
    public ImageSaveValidationSupport getImageSaveValidationSupport() {
        return saveValidationForTextImageAdSupport;
    }

    @Override
    protected ImageSize getValidImageSize() {
        return ALLOWED_SIZES_FOR_AD_IMAGE_ORIGINAL.stream().findAny().orElse(null);
    }

    @Override
    protected ImageSize getInvalidImageSize() {
        return new ImageSize().withWidth(1).withHeight(1);
    }

    @Override
    protected int getInvalidImageFileSize() {
        return ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_TEXT_IMAGE_BANNER + 1;

    }

    @Override
    protected ImageFileFormat getValidImageFileFormat() {
        return ImageFileFormat.GIF;
    }

    @Override
    protected Optional<ImageFileFormat> getInvalidImageFileFormat() {
        return Optional.of(ImageFileFormat.SVG);
    }

    @Override
    protected int getMaximumImagesPerRequest() {
        return MAX_IMAGES_PER_REQUEST_FOR_IMAGE_AD;
    }
}
