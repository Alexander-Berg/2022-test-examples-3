package ru.yandex.direct.core.entity.moderationreason.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.moderationreason.model.ModerationReason;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.utils.FunctionalUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.getSubObjectIds;
import static ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsFromDbFormat;
import static ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsToDbFormat;
import static ru.yandex.direct.utils.CommonUtils.nvl;

public class ModerationReasonMappingTest {
    @Test
    public void reasonsFromDbFormat_EmptyDoc_ReturnsEmptyList() {
        assertThat(reasonsFromDbFormat("---\n"), empty());
    }

    @Test
    public void reasonsFromDbFormat_NullDoc_ReturnsEmptyList() {
        assertThat(reasonsFromDbFormat("--- ~\n"), empty());
    }

    @Test
    public void reasonsFromDbFormat_BlankString_ReturnsEmptyList() {
        assertThat(reasonsFromDbFormat(" "), empty());
    }

    @Test
    public void reasonsFromDbFormat_OneId_ReturnsCorrectId() {
        assertThat(reasonsFromDbFormat("---\n-\n id: 1"),
                equalTo(singletonList(new ModerationReasonDetailed().withId(1L))));
    }

    @Test
    public void reasonsFromDbFormat_OneIdWithExtraData_ReturnsCorrectId() {
        assertThat(reasonsFromDbFormat("---\n-\n id: 1\n extra: 3333"),
                equalTo(singletonList(new ModerationReasonDetailed().withId(1L))));
    }

    @Test
    public void reasonsFromDbFormat_TwoId_ReturnsCorrectId() {
        assertThat(reasonsFromDbFormat("---\n-\n id: 1\n-\n id: 2"),
                equalTo(asList(new ModerationReasonDetailed().withId(1L), new ModerationReasonDetailed().withId(2L))));
    }

    @Test
    public void reasonsFromDbFormat_InvalidYaml_ReturnsEmptyList() {
        assertThat(reasonsFromDbFormat("xxx"), empty());
    }

    @Test
    public void reasonsFromDbFormat_YamlWithList_ReturnsInnerList() {
        String value =
                "-\n"
                        + "  id: 13\n"
                        + "  list:\n"
                        + "    -\n"
                        + "      id: '2238858840'\n"
                        + "      phrase: услуги -электрик\n"
                        + "    -\n"
                        + "      id: '2238858843'\n"
                        + "      phrase: Калуга\n"
                        + "    -\n"
                        + "      id: '2238858847'\n"
                        + "      phrase: вызов на дом -электрик";

        assertThat(extractSubObjectByReasons(value),
                equalTo(Map.of(13L, Set.of(2238858840L, 2238858843L, 2238858847L))));
        assertThat(listReasonsFromDbFormat(value), equalTo(asList(2238858840L, 2238858843L, 2238858847L)));
    }

    @Test
    public void reasonsFromDbFormat_YamlWithBadList_ReturnsEmptyList() {
        String value =
                "-\n"
                        + "  id: 13\n"
                        + "  list:\n";

        assertThat(extractSubObjectByReasons(value), equalTo(Map.of(13L, Set.of())));
        assertThat(listReasonsFromDbFormat(value), equalTo(emptyList()));
    }

    @Test
    public void reasonsFromDbFormat_YamlWithNullList_ReturnsEmptyList() {
        String value =
                "-\n"
                        + "  id: 13\n"
                        + "  list: ~\n";

        assertThat(extractSubObjectByReasons(value), equalTo(Map.of(13L, Set.of())));
        assertThat(listReasonsFromDbFormat(value), equalTo(emptyList()));
    }

    @Test
    public void reasonsFromDbFormat_YamlWithEmptyList_ReturnsEmptyList() {
        String value =
                "-\n"
                        + "  id: 13\n"
                        + "  list: []\n";

        assertThat(extractSubObjectByReasons(value), equalTo(Map.of(13L, Set.of())));
        assertThat(listReasonsFromDbFormat(value), equalTo(emptyList()));
    }

    @Test
    public void reasonsFromDbFormat_YamlNoInnerList_ReturnsEmptyList() {
        String value = "---\n-\n id: 1\n-\n id: 2";
        assertThat(extractSubObjectByReasons(value), equalTo(Map.of(1L, Set.of(), 2L, Set.of())));
        assertThat(listReasonsFromDbFormat(value), equalTo(emptyList()));
    }

    @Test
    public void reasonsToDbFormat_ValidList_ReturnCorrect() {
        assertThat(
                reasonsToDbFormat(asList(new ModerationReasonDetailed().withId(1L),
                        new ModerationReasonDetailed().withId(2L))),
                equalTo("---\n- id: 1\n- id: 2\n"));
    }

    @Test
    public void reasonsToDbFormat_ValidSingleList_ReturnCorrect() {
        assertThat(
                reasonsToDbFormat(singletonList(new ModerationReasonDetailed().withId(1L))),
                equalTo("---\n- id: 1\n"));
    }

    @Test
    public void reasonsToDbFormat_NullListReturnEmptyString() {
        assertThat(reasonsToDbFormat(null), equalTo(""));
    }

    @Test
    public void reasonsToDbFormat_NullInListIgnored() {
        assertThat(
                reasonsToDbFormat(asList(new ModerationReasonDetailed().withId(1L), null)),
                equalTo("---\n- id: 1\n"));
    }

    @Test
    public void reasonsToDbFormat_EmptyList_ReturnEmptyString() {
        assertThat(reasonsToDbFormat(emptyList()), equalTo(""));
    }

    @Test
    public void reasonsToDbFormat_ItemIds() {
        String expected = "---\n" +
                "- id: 1\n" +
                "  list:\n" +
                "  - id: 123\n" +
                "  - id: 234\n";
        assertThat(
                reasonsToDbFormat(List.of(new ModerationReasonDetailed().withId(1L).withItemIds(asList(123L, 234L)))),
                equalTo(expected));
    }

    @Test
    public void reasonsWithDetails() {
        String plainValue =
                "---\n"
                        + "- id: 1\n"
                        + "  comment: \"test comment\"\n"
                        + "  screenshots:\n"
                        + "  - \"/screenshot_url1\"\n"
                        + "  - \"/screenshot_url2\"\n"
                        + "- id: 2\n"
                        + "  screenshots:\n"
                        + "  - \"/screenshot_url3\"\n"
                        + "  - \"/screenshot_url4\"\n"
                        + "- id: 3\n"
                        + "  comment: \"test comment3\"\n"
                        + "- id: 4\n"
                        + "  comment: \"\"\n"
                        + "  screenshots: []\n"
                        + "- id: 5\n"
                        + "  comment: \"" + "Long comment,".repeat(50) + "\\n" + "Long comment,".repeat(50) + "\"\n"
                        + "  screenshots:\n"
                        + "  - \"/screenshot_url5\"\n";

        List<ModerationReasonDetailed> parsedValue = asList(
                new ModerationReasonDetailed()
                        .withId(1L)
                        .withComment("test comment")
                        .withScreenshots(asList("/screenshot_url1", "/screenshot_url2")),
                new ModerationReasonDetailed()
                        .withId(2L)
                        .withScreenshots(asList("/screenshot_url3", "/screenshot_url4")),
                new ModerationReasonDetailed()
                        .withId(3L)
                        .withComment("test comment3"),
                new ModerationReasonDetailed()
                        .withId(4L)
                        .withComment("")
                        .withScreenshots(emptyList()),
                new ModerationReasonDetailed()
                        .withId(5L)
                        .withComment("Long comment,".repeat(50) + "\n" + "Long comment,".repeat(50))
                        .withScreenshots(singletonList("/screenshot_url5"))
        );

        assertThat(reasonsFromDbFormat(plainValue), equalTo(parsedValue));
        assertThat(reasonsToDbFormat(parsedValue), equalTo(plainValue));
    }

    private static Map<Long, Set<Long>> extractSubObjectByReasons(String value) {
        var reasons = ModerationReasonMapping.reasonsFromDbFormat(value);
        return StreamEx.of(reasons)
                .mapToEntry(ModerationReasonDetailed::getId, ModerationReasonDetailed::getItemIds)
                .mapValues(itemIds -> nvl(itemIds, List.<Long>of()))
                .mapValues(FunctionalUtils::listToSet)
                .toMap();
    }

    private static List<Long> listReasonsFromDbFormat(String value) {
        var reasons = ModerationReasonMapping.reasonsFromDbFormat(value);
        return StreamEx.of(getSubObjectIds(new ModerationReason().withReasons(reasons)))
                .sorted()
                .collect(Collectors.toList());
    }
}
