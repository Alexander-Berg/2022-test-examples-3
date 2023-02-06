package ru.yandex.direct.core.entity.banner.type.creative;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageSizeModification;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmVideoAddition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithCreativeValidatorProviderCreativeModificationValidatorTest {

    private static final int ZERO_INDEX = 0;
    private static final long BANNER_ID = 1L;
    private static final long CREATIVE_ID_1 = 2L;
    private static final long CREATIVE_ID_2 = 3L;
    private static final long WRONG_CREATIVE_ID = -1L;
    private static final long WIDTH_1 = 10L;
    private static final long WIDTH_2 = 11L;


    private BannerWithCreativeValidatorProvider serviceUnderTest = new BannerWithCreativeValidatorProvider();

    @Parameterized.Parameter
    public Creative oldCreative;
    @Parameterized.Parameter(1)
    public Creative newCreative;
    @Parameterized.Parameter(2)
    public Long newCreativeId;
    @Parameterized.Parameter(3)
    public Class<? extends BannerWithCreative> bannerClass;
    @Parameterized.Parameter(4)
    public Defect<Void> defect;


    @Parameterized.Parameters()
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        defaultCanvasCreative(CREATIVE_ID_1),
                        defaultCanvasCreative(CREATIVE_ID_2),
                        CREATIVE_ID_2,
                        CpmBanner.class,
                        null
                },
                {
                        defaultCanvasCreative(CREATIVE_ID_1),
                        defaultCanvasCreative(CREATIVE_ID_2),
                        WRONG_CREATIVE_ID,
                        CpmBanner.class,
                        validId()
                },
                {
                        defaultCanvasCreative(CREATIVE_ID_1).withWidth(WIDTH_1),
                        defaultCanvasCreative(CREATIVE_ID_2).withWidth(WIDTH_2),
                        CREATIVE_ID_2,
                        CpmBanner.class,
                        imageSizeModification()
                },
                {
                        defaultCpmVideoAdditionCreative(CREATIVE_ID_1).withWidth(WIDTH_1),
                        defaultCanvasCreative(CREATIVE_ID_2).withWidth(WIDTH_2),
                        CREATIVE_ID_2,
                        CpmBanner.class,
                        null
                },
                {
                        defaultCanvasCreative(CREATIVE_ID_1).withIsAdaptive(true),
                        defaultCanvasCreative(CREATIVE_ID_2).withIsAdaptive(false),
                        CREATIVE_ID_2,
                        ImageBanner.class,
                        imageSizeModification()
                },
                {
                        null,
                        defaultCanvasCreative(CREATIVE_ID_2),
                        CREATIVE_ID_2,
                        CpmBanner.class,
                        null
                }

        });
    }

    private static Creative defaultCanvasCreative(long creativeId) {
        return defaultCanvas(null, null).withId(creativeId);
    }

    private static Creative defaultCpmVideoAdditionCreative(long creativeId) {
        return defaultCpmVideoAddition(null, null).withId(creativeId);
    }


    @Test
    public void validate() {

        var mc = new ModelChanges<>(BANNER_ID, bannerClass)
                .process(newCreativeId, BannerWithCreative.CREATIVE_ID)
                .castModelUp(BannerWithCreative.class);

        Map<Long, Long> oldCreativeIdByBannerId = new HashMap<>();
        Map<Long, Creative> clientCreativesByIds = new HashMap<>();
        clientCreativesByIds.put(newCreative.getId(), newCreative);

        if (oldCreative != null) {
            clientCreativesByIds.put(oldCreative.getId(), oldCreative);
            oldCreativeIdByBannerId.put(BANNER_ID, oldCreative.getId());
        }

        validateAndCheckDefect(mc, clientCreativesByIds, oldCreativeIdByBannerId, defect);
    }

    private void validateAndCheckDefect(ModelChanges<BannerWithCreative> modelChanges,
                                        Map<Long, Creative> clientCreativesByIds,
                                        Map<Long, Long> oldCreativeIdByBannerId,
                                        Defect defect) {


        ValidationResult<ModelChanges<BannerWithCreative>, Defect> validationResult =
                serviceUnderTest.creativeModificationValidator(id -> bannerClass, clientCreativesByIds,
                        oldCreativeIdByBannerId)
                        .apply(modelChanges);

        if (defect != null) {
            Path path;
            if (defect.equals(imageSizeModification())) {
                path = path();
            } else {
                path = path(field(BannerWithCreative.CREATIVE_ID));
            }
            assertThat(validationResult, hasDefectDefinitionWith(validationError(path, defect)));
        } else {
            assertThat(validationResult, hasNoDefectsDefinitions());
        }
    }
}
