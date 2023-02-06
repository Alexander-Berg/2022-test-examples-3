package ru.yandex.direct.validation.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class IndexPathNodeTest {
    @Test(expected = IllegalArgumentException.class)
    public void constructor_FailsOnNegativeIndex() {
        new PathNode.Index(-1);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("nulls")
    public void constructor_FailsOnNullIndex() {
        new PathNode.Index((Integer) null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void compareTo_WorksFine() {
        PathNode.Index index1 = new PathNode.Index(1);
        PathNode.Index index2 = new PathNode.Index(2);
        List<PathNode.Index> indexes = new ArrayList<>(asList(index2, index1));
        Collections.sort(indexes);
        assertThat(indexes, contains(sameInstance(index1), sameInstance(index2)));
    }

    @Test
    public void equals_EqualNodes_True() {
        PathNode.Index index1 = new PathNode.Index(1234);
        PathNode.Index index2 = new PathNode.Index(1234);
        assertThat(index1.equals(index2), is(true));
    }

    @Test
    public void equals_NotEqualNodes_False() {
        PathNode.Index index1 = new PathNode.Index(1234);
        PathNode.Index index2 = new PathNode.Index(1235);
        assertThat(index1.equals(index2), is(false));
    }

    @Test
    public void hashCode_EqualNodes_Equals() {
        PathNode.Index index1 = new PathNode.Index(1234);
        PathNode.Index index2 = new PathNode.Index(1234);
        assertThat(index1.hashCode(), is(index2.hashCode()));
    }

    @Test
    public void hashCode_NotEqualNodes_NotEquals() {
        PathNode.Index index1 = new PathNode.Index(1234);
        PathNode.Index index2 = new PathNode.Index(1235);
        assertThat(index1.hashCode(), is(not(index2.hashCode())));
    }

    @Test
    public void appendTo_EmptyStringBuilder_worksFine() {
        PathNode.Index index = new PathNode.Index(123);
        StringBuilder sb = new StringBuilder();
        index.appendTo(sb);
        assertThat(sb.toString(), is("[123]"));
    }

    @Test
    public void appendTo_NotEmptyStringBuilder_worksFine() {
        PathNode.Index index = new PathNode.Index(123);
        StringBuilder sb = new StringBuilder();
        sb.append("k");
        index.appendTo(sb);
        assertThat(sb.toString(), is("k[123]"));
    }
}
