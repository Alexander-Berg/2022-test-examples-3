package ru.yandex.direct.core.entity.internalads.restriction;

import java.util.Set;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageTestUtils.createBannerImageFormatDim;
import static ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageTestUtils.createBannerImageFormatEx;
import static ru.yandex.direct.core.entity.internalads.restriction.Restrictions.imageDimensionsEq;

public class RestrictionImageDimensionsEqTest {
    private static final int INCORRECT = 100;

    private static final int CORRECT_WIDTH = 10;
    private static final int CORRECT_HEIGHT = 20;

    private static final RestrictionImageDimensionsEq CHECK_CORRECT_DIM =
            imageDimensionsEq(true, CORRECT_WIDTH, CORRECT_HEIGHT);
    private static final RestrictionImageDimensionsEq CHECK_CORRECT_W = imageDimensionsEq(true, CORRECT_WIDTH, null);
    private static final RestrictionImageDimensionsEq CHECK_CORRECT_H = imageDimensionsEq(true, null, CORRECT_HEIGHT);

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectDim_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(createBannerImageFormatDim(CORRECT_WIDTH, CORRECT_HEIGHT));
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectWidth_NullResult() {
        Defect result = CHECK_CORRECT_W.check(createBannerImageFormatDim(CORRECT_WIDTH, INCORRECT));
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectHeight_NullResult() {
        Defect result = CHECK_CORRECT_H.check(createBannerImageFormatDim(INCORRECT, CORRECT_HEIGHT));
        assertThat(result).isNull();
    }

    @Test
    public void check_IncorrectDim_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(createBannerImageFormatDim(INCORRECT, INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_NOT_EQUAL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(CORRECT_WIDTH, CORRECT_HEIGHT));
    }

    @Test
    public void check_IncorrectDimWithNotCheckFormat_NullResult() {
        RestrictionImageDimensionsEq checkCorrectDim = imageDimensionsEq(true, CORRECT_WIDTH, CORRECT_HEIGHT,
                Set.of(RestrictionImageFormat.PNG));
        Defect result = checkCorrectDim.check(createBannerImageFormatEx(INCORRECT, INCORRECT, "JPEG"));
        assertThat(result).isNull();
    }

    @Test
    public void check_IncorrectDimWidthOnly_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(createBannerImageFormatDim(INCORRECT, CORRECT_HEIGHT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_NOT_EQUAL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(CORRECT_WIDTH, CORRECT_HEIGHT));
    }

    @Test
    public void check_IncorrectDimHeightOnly_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(createBannerImageFormatDim(CORRECT_WIDTH, INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_NOT_EQUAL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(CORRECT_WIDTH, CORRECT_HEIGHT));
    }

    @Test
    public void check_IncorrectWidth_NullResult() {
        Defect result = CHECK_CORRECT_W.check(createBannerImageFormatDim(INCORRECT, CORRECT_HEIGHT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_NOT_EQUAL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(CORRECT_WIDTH, null));
    }

    @Test
    public void check_IncorrectHeight_NullResult() {
        Defect result = CHECK_CORRECT_H.check(createBannerImageFormatDim(CORRECT_WIDTH, INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_NOT_EQUAL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(null, CORRECT_HEIGHT));
    }

}
