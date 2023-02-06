package ru.yandex.market.sc.core.domain.cell.policy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.IOUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author valter
 */
class DefaultCellNamePolicyTest {

    DefaultCellNamePolicy defaultCellNamePolicy = new DefaultCellNamePolicy();

    @SuppressWarnings("EqualsWithItself")
    @Test
    void sortTwoNulls() {
        assertThat(defaultCellNamePolicy.compare(null, null)).isEqualTo(0);
    }

    @Test
    void sortSingleNull() {
        assertThat(defaultCellNamePolicy.compare(null, "А")).isEqualTo(1);
        assertThat(defaultCellNamePolicy.compare("A", null)).isEqualTo(-1);
    }

    @Test
    void sortNullsLast() {
        checkSort(
                Arrays.asList("Ы", null, "G"),
                Arrays.asList("G", "Ы", null)
        );
    }

    @Test
    void sortDigitsAndNumbersDigitsWithNumbersAndNumbersWithDigitsAndNulls() {
        checkSort(
                Arrays.asList(null, "1", "A", "A-1", "1-A"),
                Arrays.asList("1", "A-1", "1-A", "A", null)
        );
    }

    @Test
    void sortWordsRussian() {
        checkSort(
                List.of("Я", "Ё", "ЁЁ"),
                List.of("Ё", "ЁЁ", "Я")
        );
    }

    @Test
    void sortWordsEnglish() {
        checkSort(
                List.of("FF", "W", "F"),
                List.of("F", "FF", "W")
        );
    }

    @Test
    void sortNumbers() {
        checkSort(
                List.of("0002", "01", "19", "5"),
                List.of("01", "0002", "5", "19")
        );
    }

    @Test
    void sortWordsWithNumbers() {
        checkSort(
                List.of("A  0002", "A -1", "A(10)", "B-2", "B1"),
                List.of("A -1", "A  0002", "A(10)", "B1", "B-2")
        );
    }

    @Test
    void sortNumbersWithWords() {
        checkSort(
                List.of("0002_ Я", "1-Э", "5)Ю", "11Д", "5Д]"),
                List.of("1-Э", "0002_ Я", "5Д]", "5)Ю", "11Д")
        );
    }

    @Test
    void sortRealWorldNames() throws Exception {
        checkSort(
                readResourceLines("cell_names/not_sorted.txt"),
                readResourceLines("cell_names/sorted.txt")
        );
    }

    private List<String> readResourceLines(String resourceName) throws IOException {
        return StreamSupport.stream(IOUtils.readLines(
                DefaultCellNamePolicyTest.class.getClassLoader()
                        .getResourceAsStream(resourceName), StandardCharsets.UTF_8).spliterator(), false)
                .toList();
    }

    private void checkSort(List<String> initial, List<String> expected) {
        var actual = new ArrayList<>(initial);
        actual.sort(defaultCellNamePolicy);
        assertThat(actual).isEqualTo(expected);
    }

}
