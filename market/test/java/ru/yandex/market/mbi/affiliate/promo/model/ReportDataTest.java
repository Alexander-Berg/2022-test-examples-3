package ru.yandex.market.mbi.affiliate.promo.model;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportDataTest {

    @Test
    public void testMerge() {
        var first = new ReportData(5,
                Map.of("Учебники", 4, "Канцтовары", 1),
                List.of(new Category(111, "Всё для школы", null)),
                null, false);
        var second = new ReportData(12,
                Map.of("Учебники", 2, "Детективы", 10),
                List.of(new Category(222, "Художественная литература", null),
                        new Category(111, "Всё для школы", null)
                        ),
                null, false);

        var result = first.merge(second);
        assertThat(result.getNumOffers()).isEqualTo(17);
        assertThat(result.getCategoriesForFilter()).containsExactlyInAnyOrder(
                new Category(222, "Художественная литература", null),
                new Category(111, "Всё для школы", null)
        );
        assertThat(result.getCategoriesForDescriptionWithNumOffers())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "Учебники", 6,
                        "Канцтовары", 1,
                        "Детективы", 10
                ));

    }

    @Test
    public void testMergeWithEmpty() {
        var first = new ReportData(5,
                Map.of("Учебники", 4, "Канцтовары", 1),
                List.of(new Category(111, "Всё для школы", null)), null, false);
        var second = new ReportData(0, Map.of(), List.of(), null, false);
        var result = second.merge(first);
        assertThat(result.getNumOffers()).isEqualTo(5);
        assertThat(result.getCategoriesForFilter())
                .containsExactlyInAnyOrder(new Category(111, "Всё для школы", null));
        assertThat(result.getCategoriesForDescriptionWithNumOffers())
                .containsExactlyInAnyOrderEntriesOf(Map.of("Учебники", 4, "Канцтовары", 1));
    }

    @Test
    public void testMergeCandidateDescriptionsWithEmpty() {
        var first = new ReportData(1,
                Map.of("Учебники", 1),
                List.of(new Category(111, "Всё для школы", null)),
                "История России 6 класс", false);
        var second = new ReportData(0, Map.of(), List.of(), null, false);
        var result = second.merge(first);
        assertThat(result.getDescriptionCandidate()).isEqualTo("История России 6 класс");
    }

    @Test
    public void testMergeCandidateDescriptions() {
        var first = new ReportData(5,
                Map.of("Учебники", 1),
                List.of(new Category(111, "Всё для школы", null)),
                "География 5 класс", false);
        var second = new ReportData(12,
                Map.of("Учебники", 1),
                List.of(new Category(111, "Всё для школы", null)
                ),
                "Биология 5 класс", false);

        var result = first.merge(second);
        assertThat(result.getDescriptionCandidate()).isNull();
    }

    @Test
    public void testMergeCandidateDescriptions2() {
        var first = new ReportData(5,
                Map.of("Учебники", 1),
                List.of(new Category(111, "Всё для школы", null)),
                "География 5 класс", false);
        var second = new ReportData(12,
                Map.of("Учебники", 2),
                List.of(new Category(111, "Всё для школы", null)
                ),
                null, false);

        var result = first.merge(second);
        assertThat(result.getDescriptionCandidate()).isNull();
    }

    @Test
    public void testMergeParents() {
        var first = new ReportData(
                5, Map.of(), List.of(
                        new Category(111, "Учебники",
                                new Category(1110, "Всё для школы", null)),
                        new Category(112, "Канцтовары",
                                new Category(1110, "Всё для школы", null))),
                null, false
        );
        var second = new ReportData(
                8, Map.of(), List.of(
                new Category(111, "Учебники",
                        new Category(1110, "Всё для школы", null)),
                new Category(2000, "Бытовая техника", null)),
                null, false
        );
        var result = first.merge(second);
        assertThat(result.getCategoriesForFilter())
                .containsExactlyInAnyOrder(
                        new Category(111, "Учебники",
                                new Category(1110, "Всё для школы", null)),
                        new Category(112, "Канцтовары",
                                new Category(1110, "Всё для школы", null)),
                        new Category(2000, "Бытовая техника", null));
    }
}