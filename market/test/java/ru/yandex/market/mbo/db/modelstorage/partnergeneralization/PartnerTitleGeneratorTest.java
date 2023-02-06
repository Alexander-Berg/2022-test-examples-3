package ru.yandex.market.mbo.db.modelstorage.partnergeneralization;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author s-ermakov
 */
@RunWith(Parameterized.class)
public class PartnerTitleGeneratorTest {

    private final String expectedTitle;
    private final List<String> titles;

    public PartnerTitleGeneratorTest(String expectedTitle, List<String> titles) {
        this.expectedTitle = expectedTitle;
        this.titles = titles;
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Parameterized.Parameters(name = "{index}: generate({1})={0}")
    public static Iterable<Object[]> data() {
        return Stream.of(
            new Group(""),
            new Group("", ""),
            new Group("Title", "title"),
            new Group("", "", ""),
            new Group("", "", "title"),
            new Group("Sku title", "Sku title"),
            new Group("Sku title 1", "Sku title 1", "Sku title 1"),
            new Group("Sku title", "Sku title 1", "Sku title 2"),
            new Group("Sku title", "Sku title 1", "Sku title 12"),
            new Group("Title1", "Title1", "Title1"),
            new Group("", "Title1", "Title2"),

            new Group("", "Рассказы ", "Сказки "),
            new Group("", "Набор игровой Barbi", "Домик Barbie", "Игрушка BARBIE \"Домик + кукла\""),
            new Group("Бальзам", "Натуральный бальзам для губ Compact с маслами Ши, Жожоба и витамином E . Голубика.", "Бальзам компакт Голубика"),
            new Group("Bubchen Молочко", "Bubchen Молочко, 400мл", "BUBCHEN Молочко 200 мл.", "Bubchen Молочко 200мл"),
            new Group("Ксилофон", "Ксилофон, 12,5х28,5 см", "Ксилофон 12,5х28,5см")
        )
            .map(Group::toParams)
            .collect(Collectors.toList());
    }

    @Test
    public void test() {
        String title = PartnerTitleGenerator.mostCommonTitle(titles);
        Assertions.assertThat(title).isEqualTo(expectedTitle);
    }

    private static class Group {
        private String expectedTitle;
        private String[] titles;

        Group(String expectedTitle, String... titles) {
            this.expectedTitle = expectedTitle;
            this.titles = titles;
        }

        Object[] toParams() {
            return new Object[] {expectedTitle, Arrays.asList(titles)};
        }
    }
}
