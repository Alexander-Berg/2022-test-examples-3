package ru.yandex.direct.core.entity.internalads.restriction;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageFormat.SVG;
import static ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageTestUtils.createBannerImageFormatEx;
import static ru.yandex.direct.core.entity.internalads.restriction.Restrictions.imageDimensionsSquare;

@ParametersAreNonnullByDefault
public class RestrictionImageDimensionsSquareTest {

    private static final RestrictionImageDimensionsSquare RESTRICTION = imageDimensionsSquare(true, Set.of(SVG));

    @Test
    public void check_NullInput_NullResult() {
        Defect result = RESTRICTION.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectDim_NullResult() {
        BannerImageFormat squareSvg = createBannerImageFormatEx(123, 123, "SVG");
        Defect result = RESTRICTION.check(squareSvg);
        assertThat(result).isNull();
    }

    @Test
    public void check_IncorrectDim_ResultWithDefect() {
        BannerImageFormat notSquareSvg = createBannerImageFormatEx(123, 321, "SVG");
        Defect result = RESTRICTION.check(notSquareSvg);
        assertThat(result)
                .isNotNull()
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_NOT_SQUARE,
                        new InternalAdRestrictionDefects.ImageDimensionParam(123, 321));
    }

    @Test
    public void check_IncorrectDimWithUnknownFormat_NullResult() {
        BannerImageFormat notSquareSvg = createBannerImageFormatEx(123, 321, "unknownFormat");
        Defect result = RESTRICTION.check(notSquareSvg);
        assertThat(result).isNull();
    }

    @Test
    public void check_IncorrectDimWithNotCheckFormat_NullResult() {
        BannerImageFormat notSquareSvg = createBannerImageFormatEx(123, 321, "PNG");
        Defect result = RESTRICTION.check(notSquareSvg);
        assertThat(result).isNull();
    }

}
