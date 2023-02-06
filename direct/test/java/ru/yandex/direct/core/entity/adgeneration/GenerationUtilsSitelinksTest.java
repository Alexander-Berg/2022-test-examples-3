package ru.yandex.direct.core.entity.adgeneration;

import java.util.Collections;
import java.util.List;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgeneration.model.SitelinkSuggest;

import static org.junit.Assert.assertEquals;

public class GenerationUtilsSitelinksTest {

    @Test
    public void emptyList() {
        List<SitelinkSuggest> input = Collections.emptyList();
        List<SitelinkSuggest> output = Collections.emptyList();
        assertEqualsSitelinks(output, GenerationUtils.sortAndSubListSitelinks(input));
    }

    @Test
    public void oneElement() {
        List<SitelinkSuggest> input = StreamEx.of(
                List.of(
                        "Пример"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        List<SitelinkSuggest> output = StreamEx.of(
                List.of(
                        "Пример"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        assertEqualsSitelinks(output, GenerationUtils.sortAndSubListSitelinks(input));
    }

    @Test
    public void oneLongElement() {
        List<SitelinkSuggest> input = StreamEx.of(
                List.of(
                        "1234567890123456789012345678901234567890123456789012345678901234567"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        List<SitelinkSuggest> output = Collections.emptyList();
        assertEqualsSitelinks(output, GenerationUtils.sortAndSubListSitelinks(input));
    }

    @Test
    public void maxLengthFourElements_oneMoreElements() {
        List<SitelinkSuggest> input = StreamEx.of(
                List.of(
                        "Длина текста   17",
                        "Длина текста 15",
                        "Длина текста     19",
                        "Длина текста  16",
                        "Длина текста    18"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        List<SitelinkSuggest> output = StreamEx.of(
                List.of(
                        "Длина текста 15",
                        "Длина текста  16",
                        "Длина текста   17",
                        "Длина текста    18",
                        "Длина текста     19"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        assertEqualsSitelinks(output, GenerationUtils.sortAndSubListSitelinks(input));
    }

    @Test
    public void maxLengthThreeElements() {
        List<SitelinkSuggest> input = StreamEx.of(
                List.of(
                        "Длина текста 15",
                        "Длина текста      20",
                        "Длина текста  16",
                        "Длина текста     19",
                        "Длина текста   17"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        List<SitelinkSuggest> output = StreamEx.of(
                List.of(
                        "Длина текста 15",
                        "Длина текста  16",
                        "Длина текста   17"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        assertEqualsSitelinks(output, GenerationUtils.sortAndSubListSitelinks(input));
    }

    @Test
    public void maxLengthFourElements_maxLengthThreeElements() {
        List<SitelinkSuggest> input = StreamEx.of(
                List.of(
                        "  3",
                        "1",
                        "Длина текста     19",
                        "Длина текста      20",
                        "Длина текста       21",
                        "Длина текста        22",
                        "Длина текста         23",
                        " 2",
                        "   4"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        List<SitelinkSuggest> output = StreamEx.of(
                List.of(
                        "1",
                        " 2",
                        "  3",
                        "   4",
                        "Длина текста     19",
                        "Длина текста      20",
                        "Длина текста       21"
                ))
                .map(title -> new SitelinkSuggest(null, title))
                .toList();
        assertEqualsSitelinks(output, GenerationUtils.sortAndSubListSitelinks(input));
    }

    public void assertEqualsSitelinks(List<SitelinkSuggest> expected, List<SitelinkSuggest> actual) {
        assertEquals("Совпадают размеры списков", expected.size(), actual.size());
        EntryStream.zip(expected, actual)
                .forEach(entry -> assertEqualsSitelink(entry.getKey(), entry.getValue()));
    }

    public void assertEqualsSitelink(SitelinkSuggest expected, SitelinkSuggest actual) {
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getUrl(), actual.getUrl());
    }

}
