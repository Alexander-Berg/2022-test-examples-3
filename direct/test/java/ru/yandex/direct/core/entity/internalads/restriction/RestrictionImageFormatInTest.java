package ru.yandex.direct.core.entity.internalads.restriction;

import java.util.Set;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageFormat.GIF;
import static ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageFormat.JPEG;
import static ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageFormat.PNG;

public class RestrictionImageFormatInTest {
    private static final RestrictionImageFormatIn CHECK_CORRECT = new RestrictionImageFormatIn(true, Set.of(PNG, GIF));

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_CORRECT.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectFormat_NullResult() {
        Defect result = CHECK_CORRECT.check(RestrictionImageTestUtils.createBannerImageFormat(PNG.getMdsFormat()));
        assertThat(result).isNull();
    }

    @Test
    public void check_IncorrectFormat_NullResult() {
        Defect result = CHECK_CORRECT.check(RestrictionImageTestUtils.createBannerImageFormat(JPEG.getMdsFormat()));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageFormat.IMAGE_FORMAT_INVALID,
                        new InternalAdRestrictionDefects.ImageFormatParam(
                                Set.of(PNG.getMdsFormat(), GIF.getMdsFormat())));
    }

    @Test
    public void check_UnknownFormat_NullResult() {
        Defect result = CHECK_CORRECT.check(RestrictionImageTestUtils.createBannerImageFormat("unknownFormat"));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageFormat.IMAGE_FORMAT_INVALID,
                        new InternalAdRestrictionDefects.ImageFormatParam(
                                Set.of(PNG.getMdsFormat(), GIF.getMdsFormat())));
    }

}
