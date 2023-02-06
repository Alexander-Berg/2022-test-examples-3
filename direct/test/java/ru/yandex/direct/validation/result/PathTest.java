package ru.yandex.direct.validation.result;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class PathTest {

    @Test
    public void isEmpty_EmptyPath() {
        Path path = new Path(new ArrayList<>());
        assertThat(path.isEmpty(), is(true));
    }

    @Test
    public void isEmpty_NotEmptyPath() {
        Path path = new Path(singletonList(new PathNode.Field("f1")));
        assertThat(path.isEmpty(), is(false));
    }

    @Test
    public void toString_EmptyPath() {
        Path path = new Path(new ArrayList<>());
        assertThat(path.toString(), is(""));
    }

    @Test
    public void toString_PathWithOneField() {
        Path path = new Path(singletonList(new PathNode.Field("f1")));
        assertThat(path.toString(), is("f1"));
    }

    @Test
    public void toString_PathWithOneIndex() {
        Path path = new Path(singletonList(new PathNode.Index(2)));
        assertThat(path.toString(), is("[2]"));
    }

    @Test
    public void toString_PathWithManyFields() {
        Path path = new Path(asList(new PathNode.Field("f1"), new PathNode.Field("e3")));
        assertThat(path.toString(), is("f1.e3"));
    }

    @Test
    public void toString_PathWithManyIndexes() {
        Path path = new Path(asList(new PathNode.Index(2), new PathNode.Index(5)));
        assertThat(path.toString(), is("[2][5]"));
    }

    @Test
    public void toString_PathWithMixedNodes() {
        Path path = new Path(asList(new PathNode.Field("f1"), new PathNode.Index(2),
                new PathNode.Field("abc"), new PathNode.Index(5), new PathNode.Index(3)));
        assertThat(path.toString(), is("f1[2].abc[5][3]"));
    }

    @Test
    public void hashCode_EmptyPaths_equals() {
        Path path1 = new Path(Collections.emptyList());
        Path path2 = new Path(Collections.emptyList());
        assertThat(path1.hashCode(), is(path2.hashCode()));
    }

    @Test
    public void hashCode_EqualPaths_equals() {
        Path path1 = new Path(singletonList(new PathNode.Field("f1")));
        Path path2 = new Path(singletonList(new PathNode.Field("f1")));
        assertThat(path1.hashCode(), is(path2.hashCode()));
    }

    @Test
    public void hashCode_DifferentPaths_notEquals() {
        Path path1 = new Path(singletonList(new PathNode.Field("f1")));
        Path path2 = new Path(singletonList(new PathNode.Field("f2")));
        assertThat(path1.hashCode(), not(is(path2.hashCode())));
    }

    @Test
    public void equals_emptyPaths_True() {
        Path path1 = new Path(Collections.emptyList());
        Path path2 = new Path(Collections.emptyList());
        assertThat(path1.equals(path2), is(true));
    }

    @Test
    public void equals_OneFieldEquals_True() {
        Path path1 = new Path(singletonList(new PathNode.Field("f1")));
        Path path2 = new Path(singletonList(new PathNode.Field("f1")));
        assertThat(path1.equals(path2), is(true));
    }

    @Test
    public void equals_OneFieldNotEquals_False() {
        Path path1 = new Path(singletonList(new PathNode.Field("f1")));
        Path path2 = new Path(singletonList(new PathNode.Field("f2")));
        assertThat(path1.equals(path2), is(false));
    }

    @Test
    public void equals_OneIndexEquals_True() {
        Path path1 = new Path(singletonList(new PathNode.Index(10)));
        Path path2 = new Path(singletonList(new PathNode.Index(10)));
        assertThat(path1.equals(path2), is(true));
    }

    @Test
    public void equals_OneIndexNotEquals_False() {
        Path path1 = new Path(singletonList(new PathNode.Index(10)));
        Path path2 = new Path(singletonList(new PathNode.Index(11)));
        assertThat(path1.equals(path2), is(false));
    }

    @Test
    public void equals_DifferentLength_False() {
        Path path1 = new Path(asList(new PathNode.Field("f1"), new PathNode.Index(2)));
        Path path2 = new Path(singletonList(new PathNode.Field("f1")));
        assertThat(path1.equals(path2), is(false));
        assertThat(path2.equals(path1), is(false));
    }

    @Test
    public void equals_MixedPathsEquals_True() {
        Path path1 = new Path(asList(new PathNode.Field("f1"), new PathNode.Index(2), new PathNode.Field("f2")));
        Path path2 = new Path(asList(new PathNode.Field("f1"), new PathNode.Index(2), new PathNode.Field("f2")));
        assertThat(path1.equals(path2), is(true));
    }

    @Test
    public void equals_MixedPathsNotEquals_False() {
        Path path1 = new Path(asList(new PathNode.Field("f1"), new PathNode.Index(2), new PathNode.Field("f2")));
        Path path2 = new Path(asList(new PathNode.Field("f1"), new PathNode.Index(2), new PathNode.Field("f3")));
        assertThat(path1.equals(path2), is(false));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getNodes_RestrictsModification() {
        Path path = new Path(singletonList(new PathNode.Field("f1")));
        path.getNodes().add(new PathNode.Field("f2"));
        assertThat(path.getNodes(), hasSize(1));
    }
}
