package ru.yandex.direct.utils;

import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.utils.ListUtils.findDuplicates;
import static ru.yandex.direct.utils.ListUtils.resample;
import static ru.yandex.direct.utils.ListUtils.uniqueList;

public class ListUtilsTest {
    @Test
    public void resample_oneItem_returnsOneItem() {
        List<Integer> result = resample(singletonList(1), 1);
        assertThat(result).containsExactly(1);
    }

    @Test
    public void resample_countMoreThanSize_returnsOriginalList() {
        List<Integer> result = resample(asList(1, 2, 3), 5);
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    public void resample_forCountOfTwo_returnsBorderElements() {
        List<Integer> result = resample(asList(1, 2, 3, 4, 5), 2);
        assertThat(result).containsExactly(1, 5);
    }

    @Test
    public void resample_forCountOfThree_returnsBorderAndMiddleElements() {
        List<Integer> result = resample(asList(1, 2, 3, 4, 5), 3);
        assertThat(result).containsExactly(1, 3, 5);
    }

    @Test
    public void resample_forNonIntegerStepShort_correctResultIsReturned() {
        List<Integer> result = resample(asList(1, 2, 3, 4, 5, 6), 4);
        assertThat(result).containsExactly(1, 2, 4, 6);
    }

    @Test
    public void resample_forNonIntegerStepLong_correctResultIsReturned() {
        List<Integer> result = resample(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), 8);
        assertThat(result).containsExactly(1, 2, 4, 5, 7, 8, 10, 12);
    }

    // Проверка значений из https://st.yandex-team.ru/DIRECT-73739
    @Test
    public void resample_returnsTheSameItemsAsPerl() {
        List<Integer> source = asList(1, 2, 3, 4, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 43, 44);
        List<Integer> result = resample(source, 5);
        assertThat(result).containsExactly(1, 7, 11, 15, 44);
    }

    @Test(expected = NullPointerException.class)
    public void findDuplicates_nullList() {
        findDuplicates(null);
    }

    @Test(expected = NullPointerException.class)
    public void findDuplicates_singletonListWithNull() {
        findDuplicates(singletonList(null));
    }

    @Test(expected = NullPointerException.class)
    public void findDuplicates_listWithNull() {
        findDuplicates(asList("1", null));
    }

    @Test
    public void findDuplicates_emptyList() {
        assertEquals(emptyList(), findDuplicates(emptyList()));
    }

    @Test
    public void findDuplicates_singletonList() {
        assertEquals(emptyList(), findDuplicates(singletonList("1")));
    }

    @Test
    public void findDuplicates_listWithDistinctValues() {
        assertEquals(emptyList(), findDuplicates(asList("1", "2", "3")));
    }

    @Test
    public void findDuplicates_listWithOneDuplicate() {
        assertEquals(singletonList("1"), findDuplicates(asList("1", "2", "1")));
    }

    @Test
    public void findDuplicates_listWithTwoDuplicates() {
        assertEquals(asList("1", "2"), findDuplicates(asList("1", "2", "1", "2")));
    }

    @Test
    public void findDuplicates_listWithTwoDuplicates_resultOrder() {
        assertEquals(asList("2", "1"), findDuplicates(asList("1", "2", "2", "1")));
    }

    @Test
    public void unique_listWithDublicates() {
        assertThat(uniqueList(asList(5, 6, 5, 5)))
                .containsExactly(5, 6);
    }
}
