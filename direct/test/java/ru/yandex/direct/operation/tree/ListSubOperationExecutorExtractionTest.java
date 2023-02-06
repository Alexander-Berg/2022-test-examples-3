package ru.yandex.direct.operation.tree;

import java.util.List;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import org.junit.Test;

import ru.yandex.direct.operation.testing.entity.ComplexTextAdGroup;
import ru.yandex.direct.operation.testing.entity.Keyword;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.operation.tree.ListSubOperationExecutor.extractChildrenSubListsToFlatList;

public class ListSubOperationExecutorExtractionTest {

    private ListMultimap<Integer, Integer> indexMap = MultimapBuilder.hashKeys().arrayListValues().build();

    // нет родителей

    @Test
    public void emptyParentsList() {
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                emptyList(), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, emptyIterable());
        assertThat(indexMap.isEmpty(), is(true));
    }

    // один родитель

    @Test
    public void oneParentWithNull() {
        ComplexTextAdGroup complexAdGroup = new ComplexTextAdGroup();
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                singletonList(complexAdGroup), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, emptyIterable());
        assertThat(indexMap.isEmpty(), is(true));
    }

    @Test
    public void oneParentWithEmptyChildren() {
        ComplexTextAdGroup complexAdGroup = new ComplexTextAdGroup()
                .withKeywords(emptyList());
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                singletonList(complexAdGroup), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, emptyIterable());
        assertThat(indexMap.isEmpty(), is(true));
    }

    @Test
    public void oneParentWithOneChild() {
        Keyword keyword = new Keyword();
        ComplexTextAdGroup complexAdGroup = new ComplexTextAdGroup()
                .withKeywords(singletonList(keyword));
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                singletonList(complexAdGroup), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, contains(sameInstance(keyword)));
        assertThat(indexMap.keySet(), hasSize(1));
        assertThat(indexMap.get(0), is(singletonList(0)));
    }

    @Test
    public void oneParentWithTwoChildren() {
        Keyword keyword1 = new Keyword();
        Keyword keyword2 = new Keyword();
        ComplexTextAdGroup complexAdGroup = new ComplexTextAdGroup()
                .withKeywords(asList(keyword1, keyword2));
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                singletonList(complexAdGroup), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, contains(asList(sameInstance(keyword1), sameInstance(keyword2))));
        assertThat(indexMap.keySet(), hasSize(1));
        assertThat(indexMap.get(0), is(asList(0, 1)));
    }

    // два родителя

    @Test
    public void extractFlatList_TwoParentsWithNulls() {
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup();
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup();
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, emptyIterable());
        assertThat(indexMap.isEmpty(), is(true));
    }

    @Test
    public void extractFlatList_TwoParentsWithEmptyChildren() {
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup()
                .withKeywords(emptyList());
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup()
                .withKeywords(emptyList());
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, emptyIterable());
        assertThat(indexMap.isEmpty(), is(true));
    }

    @Test
    public void oneParentWithOneChildAndOneWithNull() {
        Keyword keyword1 = new Keyword();
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup()
                .withKeywords(singletonList(keyword1));
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup()
                .withKeywords(null);
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, contains(sameInstance(keyword1)));
        assertThat(indexMap.keySet(), hasSize(1));
        assertThat(indexMap.get(0), is(singletonList(0)));
    }

    @Test
    public void oneParentWithNullAndAndOneWithOneChild() {
        Keyword keyword1 = new Keyword();
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup()
                .withKeywords(null);
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup()
                .withKeywords(singletonList(keyword1));
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, contains(sameInstance(keyword1)));
        assertThat(indexMap.keySet(), hasSize(1));
        assertThat(indexMap.get(1), is(singletonList(0)));
    }

    @Test
    public void oneParentWithOneChildAndOneWithEmptyChildren() {
        Keyword keyword1 = new Keyword();
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup()
                .withKeywords(singletonList(keyword1));
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup()
                .withKeywords(emptyList());
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, contains(sameInstance(keyword1)));
        assertThat(indexMap.keySet(), hasSize(1));
        assertThat(indexMap.get(0), is(singletonList(0)));
    }

    @Test
    public void oneParentWithEmptyChildrenAndAndOneWithOneChild() {
        Keyword keyword1 = new Keyword();
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup()
                .withKeywords(emptyList());
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup()
                .withKeywords(singletonList(keyword1));
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2), ComplexTextAdGroup::getKeywords);
        assertThat(flatList, contains(sameInstance(keyword1)));
        assertThat(indexMap.keySet(), hasSize(1));
        assertThat(indexMap.get(1), is(singletonList(0)));
    }

    @Test
    public void extractFlatList_TwoParentsWithSomeChildren() {
        Keyword keyword1 = new Keyword();
        Keyword keyword2 = new Keyword();
        Keyword keyword3 = new Keyword();
        Keyword keyword4 = new Keyword();
        Keyword keyword5 = new Keyword();
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup()
                .withKeywords(asList(keyword1, keyword2));
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup()
                .withKeywords(asList(keyword3, keyword4, keyword5));
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2), ComplexTextAdGroup::getKeywords);
        assertThat(flatList,
                contains(asList(sameInstance(keyword1), sameInstance(keyword2),
                        sameInstance(keyword3), sameInstance(keyword4), sameInstance(keyword5))));
        assertThat(indexMap.keySet(), hasSize(2));
        assertThat(indexMap.get(0), is(asList(0, 1)));
        assertThat(indexMap.get(1), is(asList(2, 3, 4)));
    }

    @Test
    public void twoParentsWithSomeChildrenWithHole() {
        Keyword keyword1 = new Keyword();
        Keyword keyword2 = new Keyword();
        Keyword keyword3 = new Keyword();
        ComplexTextAdGroup complexAdGroup1 = new ComplexTextAdGroup()
                .withKeywords(asList(keyword1, keyword2));
        ComplexTextAdGroup complexAdGroup2 = new ComplexTextAdGroup()
                .withKeywords(emptyList());
        ComplexTextAdGroup complexAdGroup3 = new ComplexTextAdGroup()
                .withKeywords(singletonList(keyword3));
        List<Keyword> flatList = extractChildrenSubListsToFlatList(indexMap,
                asList(complexAdGroup1, complexAdGroup2, complexAdGroup3), ComplexTextAdGroup::getKeywords);
        assertThat(flatList,
                contains(asList(sameInstance(keyword1), sameInstance(keyword2), sameInstance(keyword3))));
        assertThat(indexMap.keySet(), hasSize(2));
        assertThat(indexMap.get(0), is(asList(0, 1)));
        assertThat(indexMap.get(1), emptyIterable());
        assertThat(indexMap.get(2), is(singletonList(2)));
    }
}
