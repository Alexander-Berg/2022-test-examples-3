package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.canvas.service.video.FfmpegResolution.R_21_360p;
import static ru.yandex.canvas.service.video.FfmpegResolution.R_21_720p;
import static ru.yandex.canvas.service.video.FfmpegResolution.R_21_900p;
import static ru.yandex.canvas.service.video.FfmpegResolution.R_31_416p;


@RunWith(Parameterized.class)
public class FfmpegResolutionsConvertibleTest {

    @Parameterized.Parameter()
    public FfmpegResolutions resolutions;

    @Parameterized.Parameter(1)
    public String ratio;

    @Parameterized.Parameter(2)
    public int originWidth;

    @Parameterized.Parameter(3)
    public int originHeight;

    @Parameterized.Parameter(4)
    public FfmpegResolution[] expected;

    @Parameterized.Parameter(5)
    public String expectedString;

    @Parameterized.Parameters(name = "{index}: getConvertibleResolutions for ratio {1} originWidth {2} originHeight {3}")
    public static Iterable<Object[]> data() {
        FfmpegResolutions collection = FfmpegResolutions.of(R_21_360p, R_21_720p, R_21_900p, R_31_416p);
        return Arrays.asList(new Object[][] {
                {
                        FfmpegResolutions.of(),
                        "2:1",
                        1920,
                        1080,
                        new FfmpegResolution[] {},
                        null
                },
                {
                        collection,
                        "2:1",
                        10000,
                        10000,
                        new FfmpegResolution[] {R_21_360p, R_21_720p, R_21_900p},
                        ("[['21_360p', ['720', '360', '800', '64']]," +
                                "['21_720p', ['1440', '720', '3200', '128']]," +
                                "['21_900p', ['1800', '900', '5000', '128']]]").replace('\'', '"')
                },
                {
                        collection,
                        "2:1",
                        1800,
                        900,
                        new FfmpegResolution[] {R_21_360p, R_21_720p, R_21_900p},
                        ("[['21_360p', ['720', '360', '800', '64']]," +
                                "['21_720p', ['1440', '720', '3200', '128']]," +
                                "['21_900p', ['1800', '900', '5000', '128']]]").replace('\'', '"')
                },
                {
                        collection,
                        "2:1",
                        1801,
                        899,
                        // 1801*899 будет сконвертирован в 1800 * 900 добиванием одного пиксела сбоку
                        new FfmpegResolution[] {R_21_360p, R_21_720p, R_21_900p},
                        ("[['21_360p', ['720', '360', '800', '64']]," +
                                "['21_720p', ['1440', '720', '3200', '128']]," +
                                "['21_900p', ['1800', '900', '5000', '128']]]").replace('\'', '"')
                },
                {
                        collection,
                        "2:1",
                        1439,
                        720,
                        //  1439*720 будет сконвертирован в 1440 * 720 добиванием одного пиксела сверху/снизу
                        new FfmpegResolution[] {R_21_360p, R_21_720p},
                        ("[['21_360p', ['720', '360', '800', '64']],['21_720p', ['1440', '720', '3200', '128']]]")
                                .replace('\'', '"')
                },
                {
                        collection,
                        "2:1",
                        719,
                        359,
                        // 719*359 не будет сконвертирован в 720*360, т.к. добавлять чёрные полосы можно
                        // только с одной стороны, а тут требуется добавить с двух сторон.
                        new FfmpegResolution[] {},
                        null
                },
                {
                        collection,
                        "9:16",
                        1800,
                        900,
                        // нету разрешений с соотношением 9:16, поэтому пустой результат, не смотря на то, что
                        // по width и height - проходим
                        new FfmpegResolution[] {},
                        null
                },
        });
    }

    @Test
    public void getConvertibleResolutions() {
        Collection<FfmpegResolution> result = resolutions.getConvertibleResolutions(ratio, originWidth, originHeight);
        assertThat(result).containsExactlyInAnyOrder(expected);
        String resultString = resolutions.getConvertibleResolutionsAsString(ratio, originWidth, originHeight);
        assertThat(resultString).isEqualTo(expectedString);
    }

}
