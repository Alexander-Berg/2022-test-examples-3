package ru.yandex.direct.core.entity.internalads.restriction;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.internalads.restriction.Restrictions.imageDimensionsMax;

public class RestrictionImageDimensionsMaxTest {
    private static final int INCORRECT = 100;

    private static final int MAX_WIDTH = 10;
    private static final int MAX_HEIGHT = 20;

    private static final RestrictionImageDimensionsMax CHECK_CORRECT_DIM =
            imageDimensionsMax(true, MAX_WIDTH, MAX_HEIGHT);
    private static final RestrictionImageDimensionsMax CHECK_CORRECT_W = imageDimensionsMax(true, MAX_WIDTH, null);
    private static final RestrictionImageDimensionsMax CHECK_CORRECT_H = imageDimensionsMax(true, null, MAX_HEIGHT);

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectDim_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(RestrictionImageTestUtils.createBannerImageFormatDim(MAX_WIDTH,
                MAX_HEIGHT));
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectWidth_NullResult() {
        Defect result = CHECK_CORRECT_W.check(RestrictionImageTestUtils.createBannerImageFormatDim(MAX_WIDTH,
                INCORRECT));
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectHeight_NullResult() {
        Defect result = CHECK_CORRECT_H.check(RestrictionImageTestUtils.createBannerImageFormatDim(INCORRECT,
                MAX_HEIGHT));
        assertThat(result).isNull();
    }

    @Test
    public void check_IncorrectDim_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(RestrictionImageTestUtils.createBannerImageFormatDim(INCORRECT,
                INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_BIG,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MAX_WIDTH, MAX_HEIGHT));
    }

    @Test
    public void check_IncorrectDimWidthOnly_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(RestrictionImageTestUtils.createBannerImageFormatDim(INCORRECT,
                MAX_HEIGHT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_BIG,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MAX_WIDTH, MAX_HEIGHT));
    }

    @Test
    public void check_IncorrectDimHeightOnly_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(RestrictionImageTestUtils.createBannerImageFormatDim(MAX_WIDTH,
                INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_BIG,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MAX_WIDTH, MAX_HEIGHT));
    }

    @Test
    public void check_IncorrectWidth_NullResult() {
        Defect result = CHECK_CORRECT_W.check(RestrictionImageTestUtils.createBannerImageFormatDim(INCORRECT,
                MAX_HEIGHT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_BIG,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MAX_WIDTH, null));
    }

    @Test
    public void check_IncorrectHeight_NullResult() {
        Defect result = CHECK_CORRECT_H.check(RestrictionImageTestUtils.createBannerImageFormatDim(MAX_WIDTH,
                INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_BIG,
                        new InternalAdRestrictionDefects.ImageDimensionParam(null, MAX_HEIGHT));
    }

}
