package ru.yandex.market.mbi.affiliate.promo.report;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.mbi.affiliate.promo.model.Category;

import static org.assertj.core.api.Assertions.assertThat;


public class ReportResponseParsedTest {

    @Test
    public void testGetCategories() {
        var data = new ReportResponseParsed();
        data.intents = List.of(
                intents(111, "Бытовая техника",
                                List.of(
                                    intents(1111, "Стиральные машины", List.of()),
                                    intents(1112, "Фены", List.of(
                                            intents(11122, "Бесшумные фены", List.of())
                                    )))),
                intents(222, "Детские товары",
                                    List.of(intents(2221, "Детские книги", List.of()))),
                intents(333, "Корма для животных", List.of()));

        var result = data.getCategories(2);
        assertThat(result).containsExactlyInAnyOrder(
                new Category(1111, "Стиральные машины",
                       new Category(111, "Бытовая техника", null)),
                new Category(1112, "Фены",
                        new Category(111, "Бытовая техника", null)),
                new Category(2221, "Детские книги",
                        new Category(222, "Детские товары", null)),
                new Category(333, "Корма для животных", null)
        );
    }

    private static ReportResponseParsed.Intents intents(
            int hid, String name, List<ReportResponseParsed.Intents> children) {
        var result = new ReportResponseParsed.Intents();
        result.category = new ReportResponseParsed.CategoryParsed();
        result.category.hid = hid;
        result.category.uniqName = name;
        result.intents = children;
        return result;
    }
}