package ru.yandex.direct.validation.result;

import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PathHelperTest {

    @Test
    public void path_Empty_EmptyPath() {
        Path path = PathHelper.path();
        assertThat(path.isEmpty(), is(true));
    }

    @Test
    public void pathByStrings_MultipleNodePath() {
        String[] paths = new String[]{"1", "2", "3"};
        Path path = PathHelper.pathFromStrings(paths);
        assertThat(path.toString(), is("1.2.3"));
    }

    @Test
    public void path_OneNode_OneNodePath() {
        PathNode field = new PathNode.Field("f1");
        Path path = PathHelper.path(field);
        assertThat(path.toString(), is("f1"));
    }

    @Test
    public void path_TwoNodes_TwoNodePath() {
        PathNode field = new PathNode.Field("f1");
        PathNode index = new PathNode.Index(8);
        Path path = PathHelper.path(field, index);
        assertThat(path.toString(), is("f1[8]"));
    }

    @Test
    public void concat_PrependNode_WorksFine() {
        Path initPath = new Path(asList(new PathNode.Field("f1"), new PathNode.Index(8)));
        PathNode prependNode = new PathNode.Field("prepend");
        Path path = PathHelper.concat(prependNode, initPath);
        assertThat(path.toString(), is("prepend.f1[8]"));
    }

    @Test
    public void toPath_CollectorWorksFine() {
        List<PathNode> original = asList(new PathNode.Index(1), new PathNode.Index(2),
                new PathNode.Index(3), new PathNode.Field("f"));
        assertThat(original.stream().collect(PathHelper.toPath()), equalTo(new Path(original)));
    }
}
