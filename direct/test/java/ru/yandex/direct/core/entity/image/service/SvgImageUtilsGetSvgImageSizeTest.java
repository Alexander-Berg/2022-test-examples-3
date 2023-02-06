package ru.yandex.direct.core.entity.image.service;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankSvgImageDate;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankSvgImageDateWithoutLeadingXmlTag;

@RunWith(Parameterized.class)
public class SvgImageUtilsGetSvgImageSizeTest {
    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public byte[] imageBytes;

    @Parameterized.Parameter(2)
    public Integer width;

    @Parameterized.Parameter(3)
    public Integer height;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"xml svg with width/height", generateBlankSvgImageDate(100, 200), 100, 200},
                {"svg with width/height", generateBlankSvgImageDateWithoutLeadingXmlTag(100, 200), 100, 200},
                {"svg with viewBox", "<svg viewBox=\"0 0 100 200\"/>".getBytes(), 100, 200},
                {"svg with fractional viewBox 1", "<svg viewBox=\"0.2 0 100.1 200.1\"/>".getBytes(), 100, 200},
                {"svg with fractional viewBox 2", "<svg viewBox=\"0 0.1 99.5 199.8\"/>".getBytes(), 100, 200},

                {"svg with mm",
                        "<svg width=\"20mm\" height=\"20mm\" viewBox=\"0 0.1 99.5 199.8\"/>".getBytes(), null, null},
        });
    }

    @Test
    public void checkMimeTypeDetection() {
        var imageSize = SvgImageUtils.getSvgImageSize(imageBytes);
        if (width != null && height != null) {
            assertThat(imageSize.isPresent()).isTrue();
            assertThat(imageSize.get().getWidth()).isEqualTo(width);
            assertThat(imageSize.get().getHeight()).isEqualTo(height);
        } else {
            assertThat(imageSize.isEmpty()).isTrue();
        }
    }
}
