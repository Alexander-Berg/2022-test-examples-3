package ru.yandex.direct.operation.tree;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.operation.tree.ItemSubOperationExecutor.extractSubList;

public class ItemSubOperationExecutorExtractionTest {

    private List<Boolean> sourceList;
    private Map<Integer, Integer> sourceToDestIndexMap = new HashMap<>();
    private List<Boolean> destList;

    @Test
    public void noElementsFoundInListWithSomeElements() {
        sourceList = Arrays.asList(false, false, false);
        extract();
        checkDestList(0);
        assertThat(sourceToDestIndexMap.keySet(), emptyIterable());
    }

    @Test
    public void worksWithEmptyList() {
        sourceList = emptyList();
        extract();
        checkDestList(0);
        assertThat(sourceToDestIndexMap.keySet(), emptyIterable());
    }

    @Test
    public void noElementsFoundInFilledListWithNulls() {
        sourceList = Arrays.asList(null, false, null, false, null);
        extract();
        checkDestList(0);
        assertThat(sourceToDestIndexMap.keySet(), emptyIterable());
    }

    @Test
    public void oneElementFoundAtZeroIndexInListWithOneElement() {
        sourceList = singletonList(true);
        extract();
        checkDestList(1);
        checkIndexMap(0, 0);
        checkIndexMapSize(1);
    }

    @Test
    public void oneElementFoundAtZeroIndexInListWithTwoElements() {
        sourceList = Arrays.asList(true, false);
        extract();
        checkDestList(1);
        checkIndexMap(0, 0);
        checkIndexMapSize(1);
    }

    @Test
    public void oneElementFoundAtFirstIndexInListWithTwoElements() {
        sourceList = Arrays.asList(false, true);
        extract();
        checkDestList(1);
        checkIndexMap(1, 0);
        checkIndexMapSize(1);
    }

    @Test
    public void oneElementFoundInListWithTwoElementsWithNull() {
        sourceList = Arrays.asList(null, true);
        extract();
        checkDestList(1);
        checkIndexMap(1, 0);
        checkIndexMapSize(1);
    }

    @Test
    public void oneElementFoundAtFirstIndexInListWithThreeElements() {
        sourceList = Arrays.asList(false, true, false);
        extract();
        checkDestList(1);
        checkIndexMap(1, 0);
        checkIndexMapSize(1);
    }

    @Test
    public void twoElementsFoundInListWithTwoElements() {
        sourceList = Arrays.asList(true, true);
        extract();
        checkDestList(2);
        checkIndexMap(0, 0);
        checkIndexMap(1, 1);
        checkIndexMapSize(2);
    }

    @Test
    public void twoElementsFoundInBeginningOfListWithThreeElements() {
        sourceList = Arrays.asList(true, true, false);
        extract();
        checkDestList(2);
        checkIndexMap(0, 0);
        checkIndexMap(1, 1);
        checkIndexMapSize(2);
    }

    @Test
    public void twoElementsFoundInEndOfListWithThreeElements() {
        sourceList = Arrays.asList(false, true, true);
        extract();
        checkDestList(2);
        checkIndexMap(1, 0);
        checkIndexMap(2, 1);
        checkIndexMapSize(2);
    }

    @Test
    public void twoElementsFoundInListWithThreeElementsSeparated() {
        sourceList = Arrays.asList(true, false, true);
        extract();
        checkDestList(2);
        checkIndexMap(0, 0);
        checkIndexMap(2, 1);
        checkIndexMapSize(2);
    }

    @Test
    public void twoElementsFoundInListWithThreeElementsSeparatedWithNull() {
        sourceList = Arrays.asList(true, null, true, false, null);
        extract();
        checkDestList(2);
        checkIndexMap(0, 0);
        checkIndexMap(2, 1);
        checkIndexMapSize(2);
    }

    private void extract() {
        destList = extractSubList(sourceToDestIndexMap, sourceList, Boolean.TRUE::equals);
    }

    private void checkDestList(int size) {
        assertThat(destList, hasSize(size));

        List<Boolean> expectedList = nCopies(size, true);
        assertThat(destList, beanDiffer(expectedList));
    }

    private void checkIndexMap(int sourceIndex, int destIndex) {
        assertThat("результирующая мапа индексов не содержит индекс исходного списка: " + sourceIndex,
                sourceToDestIndexMap.containsKey(sourceIndex), is(true));
        assertThat("результирующая мапа индексов содержит неправильный индекс "
                        + "в результирующем списке для индекса в исходном списке: " + sourceIndex,
                sourceToDestIndexMap.get(sourceIndex), equalTo(destIndex));
    }

    private void checkIndexMapSize(int size) {
        assertThat(sourceToDestIndexMap.keySet(), hasSize(size));
    }
}
