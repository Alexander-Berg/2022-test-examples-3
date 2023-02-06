package ru.yandex.canvas.service.video;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.Size;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.VideoLimitsInterface;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.CommonUtils.nvl;

/**
 * Проверки для DirectFfmpegResolutions.OUTDOOR форматов, связанные с outdoor рекомендациями.
 * FfmpegResolution - разрешения на которые будут нарезаться креативы.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class FfmpegOutdoorRecommendationsTest {

    private static final String FAIL_MESSAGE = "Рекомендуемое разрешение будет превышать максимальное разрешение";
    private static final FfmpegResolutions outdoorFfmpegResolutions = DirectFfmpegResolutions.OUTDOOR;
    private VideoLimitsInterface limits;
    @MockBean
    private AuthRequestParams authRequestParams;
    @MockBean
    private DirectService directService;

    @Before
    public void setUp() {
        VideoLimitsService videoLimitsService = new VideoLimitsService(authRequestParams, directService);
        limits = videoLimitsService.getLimits(VideoCreativeType.CPM_OUTDOOR, null);
    }

    /**
     * Проверить, что среди outdoor разрешений для нарезки нет нестандартных разрешений, у которых реальное
     * ratio будет меньше чем ratio в FfmpegResolution.
     */
    @Test
    public void checkThatRealRatioNotLessThanFfmpegRatio() {

        outdoorFfmpegResolutions.getAllResolutions()
                .forEach(x -> {
                    Ratio realRatio = new Ratio(x.getWidth(), x.getHeight());

                    String[] wxh = x.getRatio().split(":");
                    int ffmpegRatioWidth = Integer.parseInt(wxh[0]);
                    int ffmpegRatioHeight = Integer.parseInt(wxh[1]);

                    assertThat(realRatio.getWidth() * realRatio.getHeight())
                            .isGreaterThanOrEqualTo(ffmpegRatioWidth * ffmpegRatioHeight);

                });
    }

    /**
     * Проверить, что рекомендуемое разрешение не будет больше максимального разрешения.
     * Для чего:
     * Рекомендуем пользователям загрузить разрешение, которое нарежется под максимальное количество щитов.
     * Чтобы определить максимальное разрешение - берем максимальное разрешение из FfmpegResolution для каждого ratio.
     * Но если максимальное разрешение из FfmpegResolution нестандартное, то пользователь не сможет загрузить такое
     * видео,
     * поэтому в некоторых ситуациях рекомендуем чуть больше
     * (см. GridOutdoorVideoRecommendationForPlacementsService.increaseSidesToExactRatio)
     */
    @Test
    public void checkMaxLimitAfterIncreaseSides() {
        Map<String, List<FfmpegResolution>> groupedByRatio = outdoorFfmpegResolutions.getAllResolutions()
                .stream()
                .collect(Collectors.groupingBy(FfmpegResolution::getRatio));

        groupedByRatio.forEach((ratio, resolutions) -> {
            FfmpegResolution maxResolution =
                    resolutions.stream().max(Comparator.comparingInt(x -> x.getWidth() * x.getHeight())).orElseThrow();

            Size recommended = increaseSidesToExactRatio(maxResolution.getWidth(), maxResolution.getHeight(), ratio);
            Size maxSize = getMaxSize(ratio);

            assertThat(recommended.getWidth()).withFailMessage(FAIL_MESSAGE).isLessThanOrEqualTo(maxSize.getWidth());
            assertThat(recommended.getHeight()).withFailMessage(FAIL_MESSAGE).isLessThanOrEqualTo(maxSize.getHeight());
        });
    }

    private Size getMaxSize(String ratio) {
        Map<String, Integer> limitsByRatio =
                (Map<String, Integer>) limits.getDurationLimitsByRatio()
                        .getOrDefault(ratio, emptyMap());
        Integer ratioSpecificMaxWidth = limitsByRatio.get("maxWidth");
        Integer ratioSpecificMaxHeight = limitsByRatio.get("maxHeight");

        Integer maxWidth = nvl(ratioSpecificMaxWidth, limits.getVideoWidthMax());
        Integer maxHeight = nvl(ratioSpecificMaxHeight, limits.getVideoHeightMax());
        return Size.of(maxWidth, maxHeight);
    }

    /**
     * см. GridOutdoorVideoRecommendationForPlacementsService.increaseSidesToExactRatio
     */
    private Size increaseSidesToExactRatio(int currentWidth, int currentHeight, String ratioString) {
        String[] wxh = ratioString.split(":");
        int ratioWidth = Integer.parseInt(wxh[0]);
        int ratioHeight = Integer.parseInt(wxh[1]);

        // обе стороны в любом случае должны быть кратны соотвуствующей стороне ratio
        int remainderWidth = currentWidth % ratioWidth;
        int remainderHeight = currentHeight % ratioHeight;
        if (remainderWidth != 0) {
            currentWidth += ratioWidth - remainderWidth;
        }
        if (remainderHeight != 0) {
            currentHeight += ratioHeight - remainderHeight;
        }

        // определяем множители для сторон (если соотношение сторон точно равно ratio, то множители не будут
        // отличаться)
        int widthMultiplier = currentWidth / ratioWidth;
        int heightMultiplier = currentHeight / ratioHeight;
        // Увеличиваем сторону, множитель которой оказался меньше (в обратном случае одна из сторон уменьшится).
        if (widthMultiplier > heightMultiplier) {
            currentHeight = widthMultiplier * ratioHeight;
        } else {
            currentWidth = heightMultiplier * ratioWidth;
        }

        return Size.of(currentWidth, currentHeight);
    }

}
