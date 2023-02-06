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
public class FfmpegResolutionsAllTest {

    @Parameterized.Parameter()
    public FfmpegResolutions resolutions;

    @Parameterized.Parameter(1)
    public String ratio;

    @Parameterized.Parameter(2)
    public FfmpegResolution[] expected;

    @Parameterized.Parameter(3)
    public String expectedString;

    @Parameterized.Parameters(name = "{index}: getAllResolutions for ratio {1}")
    public static Iterable<Object[]> data() {
        FfmpegResolutions resolutions = FfmpegResolutions.of(R_21_360p, R_21_720p, R_21_900p, R_31_416p);
        return Arrays.asList(new Object[][] {
                {
                        FfmpegResolutions.of(),
                        "2:1",
                        new FfmpegResolution[] {},
                        null
                },
                {
                        resolutions,
                        "2:1",
                        new FfmpegResolution[] {R_21_360p, R_21_720p, R_21_900p},
                        ("[['21_360p', ['720', '360', '800', '64']]," +
                                "['21_720p', ['1440', '720', '3200', '128']]," +
                                "['21_900p', ['1800', '900', '5000', '128']]]").replace('\'', '"')
                },
                {
                        resolutions,
                        "3:1",
                        new FfmpegResolution[] {R_31_416p},
                        ("[['31_416p', ['1248', '416', '2000', '64']]]").replace('\'', '"')
                },
                {
                        resolutions,
                        "9:16",
                        new FfmpegResolution[] {},
                        null
                }
        });
    }

    @Test
    public void getAllResolutions_FilteredByRatio() {
        Collection<FfmpegResolution> result = resolutions.getAllResolutions(ratio);
        assertThat(result).containsExactlyInAnyOrder(expected);
        String resultString = resolutions.getAllResolutionsAsString(ratio);
        assertThat(resultString).isEqualTo(expectedString);
    }

}
