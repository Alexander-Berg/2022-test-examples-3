package ru.yandex.direct.core.entity.internalads.restriction;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.internalads.restriction.Restrictions.imageDimensionsMin;

public class RestrictionImageDimensionsMinTest {
    private static final int INCORRECT = 5;

    private static final int MIN_WIDTH = 10;
    private static final int MIN_HEIGHT = 20;

    private static final RestrictionImageDimensionsMin CHECK_CORRECT_DIM =
            imageDimensionsMin(true, MIN_WIDTH, MIN_HEIGHT);
    private static final RestrictionImageDimensionsMin CHECK_CORRECT_W = imageDimensionsMin(true, MIN_WIDTH, null);
    private static final RestrictionImageDimensionsMin CHECK_CORRECT_H = imageDimensionsMin(true, null, MIN_HEIGHT);

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectDim_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(RestrictionImageTestUtils.createBannerImageFormatDim(MIN_WIDTH,
                MIN_HEIGHT));
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectWidth_NullResult() {
        Defect result = CHECK_CORRECT_W.check(RestrictionImageTestUtils.createBannerImageFormatDim(MIN_WIDTH,
                INCORRECT));
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectHeight_NullResult() {
        Defect result = CHECK_CORRECT_H.check(RestrictionImageTestUtils.createBannerImageFormatDim(INCORRECT,
                MIN_HEIGHT));
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
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_SMALL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MIN_WIDTH, MIN_HEIGHT));
    }

    @Test
    public void check_IncorrectDimWidthOnly_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(RestrictionImageTestUtils.createBannerImageFormatDim(INCORRECT,
                MIN_HEIGHT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_SMALL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MIN_WIDTH, MIN_HEIGHT));
    }

    @Test
    public void check_IncorrectDimHeightOnly_NullResult() {
        Defect result = CHECK_CORRECT_DIM.check(RestrictionImageTestUtils.createBannerImageFormatDim(MIN_WIDTH,
                INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_SMALL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MIN_WIDTH, MIN_HEIGHT));
    }

    @Test
    public void check_IncorrectWidth_NullResult() {
        Defect result = CHECK_CORRECT_W.check(RestrictionImageTestUtils.createBannerImageFormatDim(INCORRECT,
                MIN_HEIGHT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_SMALL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(MIN_WIDTH, null));
    }

    @Test
    public void check_IncorrectHeight_NullResult() {
        Defect result = CHECK_CORRECT_H.check(RestrictionImageTestUtils.createBannerImageFormatDim(MIN_WIDTH,
                INCORRECT));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_SMALL,
                        new InternalAdRestrictionDefects.ImageDimensionParam(null, MIN_HEIGHT));
    }

}
