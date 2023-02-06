package ru.yandex.market.supportwizard.vippartners;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class VipPartnerDescriptionBlockUtilTest {

    @ParameterizedTest
    @MethodSource("parseTracesArgs")
    void parseTracesFromDescriptionTest(String caption, String description, Collection<String> expected) {
        Assertions.assertEquals(expected, VipPartnerDescriptionBlockUtil.parseTracesFromDescription(description));
    }

    private static Stream<Arguments> parseTracesArgs() {
        return Stream.of(
                Arguments.of("Описание только из списка трассировок",
                        "<{последние трассировки\n"
                        + "  https://tsum.yandex-team.ru/t1/t2\n"
                        + "  https://tsum.yandex-team.ru/t3/t4\n"
                        + "  fdds/asdf\n"
                        + "}>",
                        List.of("https://tsum.yandex-team.ru/t1/t2", "https://tsum.yandex-team.ru/t3/t4", "fdds/asdf")),
                Arguments.of("Описание из разной информации",
                                "Тикет обо всем"
                                + "#|"
                                + "|| колонка 1 | колонка 2 ||"
                                + "|| данные 1 | данные 2 ||"
                                + "|#"
                                + ""
                                + "<{sql запрос"
                                + "select * form shops"
                                + "where 1=0"
                                + "}>"
                                + "<{последние трассировки\n"
                                + "  https://tsum.yandex-team.ru/t1/t2\n"
                                + "  https://tsum.yandex-team.ru/t3/t4\n"
                                + "  fdds/asdf\n"
                                + "}>",
                        List.of("https://tsum.yandex-team.ru/t1/t2", "https://tsum.yandex-team.ru/t3/t4", "fdds/asdf")),
                Arguments.of("Описание с неправильным катом",
                        "<{предпоследние трассировки\n"
                                + "  https://tsum.yandex-team.ru/t1/t2\n"
                                + "  https://tsum.yandex-team.ru/t3/t4\n"
                                + "  fdds/asdf\n"
                                + "}>", Collections.emptyList()),
                Arguments.of("Описание без ката с трассировками",
                        "Никаких трассировок нет", Collections.emptyList()),
                Arguments.of("Описание с пустым катом трассировок", "<{последние трассировки\n"
                        + "}>", Collections.emptyList())
        );
    }

    @ParameterizedTest
    @MethodSource("appendTraces")
    void appendTracesTest(String caption, Collection<String> existingTraces, Collection<String> newTraces, int maxSize,
            Collection<String> expected) {
        Assertions.assertEquals(expected,
                VipPartnerDescriptionBlockUtil.appendTraces(existingTraces, newTraces, maxSize));
    }

    private static Stream<Arguments> appendTraces() {
        return Stream.of(
                Arguments.of("Add empty to empty",
                        Collections.emptyList(), Collections.emptyList(), 10, Collections.emptyList()),
                Arguments.of("Add to empty",
                        Collections.emptyList(), Arrays.asList("a/b", "c/d"), 2, Arrays.asList("a/b", "c/d")),
                Arguments.of("Add oversize to empty",
                        Collections.emptyList(), Arrays.asList("a/b", "c/d", "e/f"), 2, Arrays.asList("c/d", "e/f")),
                Arguments.of("Add without oversize", Collections.singletonList("a/b"),
                        Collections.singletonList("c/d"), 2, Arrays.asList("a/b", "c/d")),
                Arguments.of("Oversize, remove existing", Collections.singletonList("a/b"),
                        Arrays.asList("c/d", "e/f"), 2, Arrays.asList("c/d", "e/f")),
                Arguments.of("Oversize, remove existing and new", Collections.singletonList("a/b"),
                        Arrays.asList("c/d", "e/f", "g/h"), 2, Arrays.asList("e/f", "g/h"))
        );
    }

    @ParameterizedTest
    @MethodSource("generateTracesBlock")
    void generateTracesBlockTest(String caption, Collection<String> traces, String expected) {
        Assertions.assertEquals(expected, VipPartnerDescriptionBlockUtil.generateDescriptionTracesBlock(traces));
    }

    private static Stream<Arguments> generateTracesBlock() {
        return Stream.of(
                Arguments.of("Generate traces block",
                        Arrays.asList("https://tsum.yandex-team.ru/t1/t2", "https://tsum.yandex-team.ru/t3/t4"),
                        "<{последние трассировки\n"
                        + "  https://tsum.yandex-team.ru/t1/t2\n"
                        + "  https://tsum.yandex-team.ru/t3/t4\n"
                        + "}>"),
                Arguments.of("Generate empty traces block",
                        Collections.emptyList(),
                        "<{последние трассировки\n\n}>")
        );
    }

    @ParameterizedTest
    @MethodSource("generateDescriptionTracesBlock")
    void generateDescriptionTracesBlockTest(String caption, String existingDescription, Collection<String> newTraces, String expected) {
        Assertions.assertEquals(expected, VipPartnerDescriptionBlockUtil.generateDescriptionTracesBlock(existingDescription, newTraces));
    }

    private static Stream<Arguments> generateDescriptionTracesBlock() {
        return Stream.of(
                Arguments.of("Добавить новые трассировки", "<{последние трассировки\n"
                                + "  https://tsum.yandex-team.ru/t1/t2\n"
                                + "  https://tsum.yandex-team.ru/t3/t4\n"
                                + "  fdds/asdf\n"
                                + "}>",
                        List.of("ab/cd", "ef/gh"),
                        "<{последние трассировки\n"
                                + "  https://tsum.yandex-team.ru/t1/t2\n"
                                + "  https://tsum.yandex-team.ru/t3/t4\n"
                                + "  fdds/asdf\n"
                                + "  ab/cd\n"
                                + "  ef/gh\n"
                                + "}>"),
                Arguments.of("Добавить пустой список новых трассировок",
                        "<{последние трассировки\n"
                                + "  https://tsum.yandex-team.ru/t1/t2\n"
                                + "  https://tsum.yandex-team.ru/t3/t4\n"
                                + "  fdds/asdf\n"
                                + "}>",
                        Collections.emptyList(),
                        "<{последние трассировки\n"
                                + "  https://tsum.yandex-team.ru/t1/t2\n"
                                + "  https://tsum.yandex-team.ru/t3/t4\n"
                                + "  fdds/asdf\n"
                                + "}>"),
                Arguments.of("Добавить новые трассировки к пустому списку",
                        "Никаких трассировок нет", List.of("ab/cd", "ef/gh"),
                        "<{последние трассировки\n"
                                + "  ab/cd\n"
                                + "  ef/gh\n"
                                + "}>"),
                Arguments.of("Добавить пустой список новых трассировок к пустому списку",
                        "<{последние трассировки\n}", Collections.emptyList(), "<{последние трассировки\n\n}>")
        );
    }
}
