package ru.yandex.direct.core.entity.internalads.restriction;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.internalads.restriction.Restrictions.imageSizeKbMax;

public class RestrictionImageSizeKbMaxTest {

    private static final int MAX_SIZE = 30;
    private static final RestrictionImageSizeKbMax CHECK_CORRECT = imageSizeKbMax(true, MAX_SIZE);

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_CORRECT.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectDim_NullResult() {
        Defect result = CHECK_CORRECT.check(RestrictionImageTestUtils.createBannerImageFormatFileSize(MAX_SIZE));
        assertThat(result).isNull();
    }

    @Test
    public void check_IncorrectDim_NullResult() {
        Defect result = CHECK_CORRECT.check(RestrictionImageTestUtils.createBannerImageFormatFileSize(MAX_SIZE + 1));
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(
                        InternalAdRestrictionDefects.ImageSize.IMAGE_SIZE_KB_TOO_BIG,
                        new InternalAdRestrictionDefects.ImageSizeKbParam(MAX_SIZE));
    }
}
