package ru.yandex.direct.validation.result;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class FieldPathNodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void constructor_FailsOnNullString() {
        new PathNode.Field(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_FailsOnEmptyString() {
        new PathNode.Field("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_FailsOnBlankString() {
        new PathNode.Field(" \t");
    }

    @Test
    public void equals_EqualNodes_True() {
        PathNode.Field index1 = new PathNode.Field("key");
        PathNode.Field index2 = new PathNode.Field("key");
        assertThat(index1.equals(index2), is(true));
    }

    @Test
    public void equals_NotEqualNodes_False() {
        PathNode.Field index1 = new PathNode.Field("key");
        PathNode.Field index2 = new PathNode.Field("hey");
        assertThat(index1.equals(index2), is(false));
    }

    @Test
    public void hashCode_EqualNodes_Equals() {
        PathNode.Field index1 = new PathNode.Field("key");
        PathNode.Field index2 = new PathNode.Field("key");
        assertThat(index1.hashCode(), is(index2.hashCode()));
    }

    @Test
    public void hashCode_NotEqualNodes_NotEquals() {
        PathNode.Field index1 = new PathNode.Field("key");
        PathNode.Field index2 = new PathNode.Field("hey");
        assertThat(index1.hashCode(), is(not(index2.hashCode())));
    }

    @Test
    public void appendTo_EmptyStringBuilder_worksFine() {
        PathNode.Field index = new PathNode.Field("key");
        StringBuilder sb = new StringBuilder();
        index.appendTo(sb);
        assertThat(sb.toString(), is("key"));
    }

    @Test
    public void appendTo_NotEmptyStringBuilder_worksFine() {
        PathNode.Field index = new PathNode.Field("key");
        StringBuilder sb = new StringBuilder();
        sb.append("p");
        index.appendTo(sb);
        assertThat(sb.toString(), is("p.key"));
    }
}
