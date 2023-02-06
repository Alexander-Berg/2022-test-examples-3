package ru.yandex.market.mboc.common;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.availability.PeriodResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author yuramalinov
 * @created 04.09.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MbocErrorsTest {
    @Test
    public void testMbocErrorsCanLoad() {
        assertNotNull(MbocErrors.get().internalMatchingMskuIsAbsent());
    }

    @Test
    public void testParamsArePassed() {
        ErrorInfo errorInfo = MbocErrors.get().excelMaxLength("header", "Something", 5);
        assertEquals(errorInfo.getParams(),
            ImmutableMap.of("value", "Something", "maxLength", 5, "header", "header"));
    }

    @Test
    public void testRender() {
        ErrorInfo errorInfo = MbocErrors.get().excelMaxLength("header", "Something", 5);
        assertEquals("Значение 'Something' для колонки 'header' превышает максимальную длину 5 символов",
            errorInfo.toString());
    }

    @Test
    public void testRenderList() {
        ErrorInfo errorInfo = MbocErrors.get().notAllPicturesDownloaded("ssku", List.of("1", "2", "3"));
        assertEquals("Не все картинки для оффера ssku загружены. urls = [1, 2, 3]",
            errorInfo.toString());
    }

    @Test
    public void testRenderProcessed() {
        ErrorInfo errorInfo = MbocErrors.get().excelMaxLength("header", "Something", 5);
        assertEquals("Значение 'Something!!!' для колонки 'header!!!' превышает максимальную длину 5 символов",
            errorInfo.render(o -> o instanceof String ? o + "!!!" : o));
    }

    @Test
    public void testEverythingCanCompile() {
        MbocErrors.Holder.methodToAnnotation.values()
            .forEach(template -> ErrorInfo.tryCompile(template.message()));
    }

    @Test
    public void testNoDuplicates() {
        List<String> whiteListedDuplicates = List.of("mboc.error.excel-value-not-in-range");
        List<Map.Entry<String, Long>> duplicates = MbocErrors.Holder.methodToAnnotation.values().stream()
            .map(MbocErrors.ErrorTemplate::code)
            .filter(Predicate.not(whiteListedDuplicates::contains))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .collect(Collectors.toList());

        assertThat(duplicates).isEmpty();
    }

    @Test
    public void testProcessor() {
        ErrorInfo errorInfo = MbocErrors.get().mskuNotAvailableForDeliverySeasonPeriod(List.of(
            new PeriodResponse(5, 6, "5", "6", "", "").setLast(false),
            new PeriodResponse(1, 3, "1", "3", "", "").setLast(true),
            new PeriodResponse(3, 5, "3", "5", "", "").setLast(false)
        ).stream().sorted(PeriodResponse.COMPARATOR).collect(Collectors.toList()), 998, "warehouse-name");

        assertThat(errorInfo.getParams()).containsKey("periods");
        @SuppressWarnings("unchecked")
        List<PeriodResponse> periods = (List<PeriodResponse>) errorInfo.getParams().get("periods");
        assertThat(periods).isSortedAccordingTo(
            Comparator.comparing(PeriodResponse::getFrom)
                .thenComparing(PeriodResponse::getTo));
        assertThat(periods).extracting(PeriodResponse::isLast)
            .containsExactly(false, false, true);


        assertThat(errorInfo.toString()).contains("1 - 3, 3 - 5, 5 - 6");
    }

}
