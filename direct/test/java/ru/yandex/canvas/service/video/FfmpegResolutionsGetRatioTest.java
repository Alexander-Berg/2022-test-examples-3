package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class FfmpegResolutionsGetRatioTest {

    private final FfmpegResolutions ffmpegResolutions = FfmpegResolutions.of(FfmpegResolution.values());

    @Parameterized.Parameter(0)
    public int width;

    @Parameterized.Parameter(1)
    public int height;

    @Parameterized.Parameter(2)
    public Boolean isExist;

    @Parameterized.Parameter(3)
    public String expectedRatio;

    @Parameterized.Parameters(name = "width: {0}, height: {1}, isExist {2}, ratio {3}")
    public static Iterable<Object[]> data() {
        List<Object[]> data = Arrays.stream(FfmpegResolution.values())
                .map(x -> new Object[]{x.getWidth(), x.getHeight(), true, x.getRatio()})
                .collect(toList());

        data.add(new Object[]{-1, -1, false, null});
        return data;
    }

    @Test
    public void getResolutionRatio_RatioIsCorrect() {
        Optional<String> ratio = ffmpegResolutions.getResolutionRatio(width, height);
        assertThat(ratio.isPresent()).isEqualTo(isExist);
        ratio.ifPresent(s -> assertThat(s).isEqualTo(expectedRatio));
    }
}
