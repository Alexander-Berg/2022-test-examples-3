package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class FfmpegResolutionTest {

    @Test
    public void suffixMustBeUnique() {
        Map<String, Set<FfmpegResolution>> suffixToResolution = Arrays.stream(FfmpegResolution.values())
                .collect(Collectors.groupingBy(FfmpegResolution::getSuffix, Collectors.toSet()));
        Optional<Map.Entry<String, Set<FfmpegResolution>>> sameSuffix = suffixToResolution.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .findFirst();
        sameSuffix.ifPresent(suffixAndResolutions ->
                fail(String.format("Entries with the same suffix %s found: %s",
                        suffixAndResolutions.getKey(), suffixAndResolutions.getValue().toString())));
    }

    /**
     * Проверяет что название enum, width, height и ratio согласованы.
     * Тест призван ловить опечатки в разрешениях.
     * Например при добавлении нового разрешения можно откопировать предыдущюю строчку, но забыть поменять
     * одно из чисел.
     *
     * Тест не проверяет suffix, т.к. он ни на что не влияет, кроме имени файла и в принципе может быть любым.
     * Можно когда-нибудь включить проверку суффиксов тоже, но на момент написания теста был один суффикс
     * "неправильно" названный, поэтому пока что без этой проверки.
     */
    @Test
    public void allEnumValuesAreConsistent() {
        for (var val : FfmpegResolution.values()) {
            String[] ratios = val.getRatio().split(":");
            int widthRatio = Integer.parseInt(ratios[0]);
            int heightRatio = Integer.parseInt(ratios[1]);
            int width = val.getWidth();
            int height = val.getHeight();
            boolean exactResolution = widthRatio * height == heightRatio * width;
            String expectedSuffixName = String.format("%s_%sp%s",
                    "" + widthRatio + heightRatio,
                    height,
                    exactResolution ? "" : "_" + width);
            String expectedEnumName = "R_" + expectedSuffixName;
            var enumValue = FfmpegResolution.valueOf(expectedEnumName);

            assertThat(val).isEqualTo(enumValue);
        }
    }

}
