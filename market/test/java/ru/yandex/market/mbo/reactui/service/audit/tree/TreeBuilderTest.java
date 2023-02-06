package ru.yandex.market.mbo.reactui.service.audit.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static ru.yandex.market.mbo.reactui.service.audit.tree.TreeBuilder.node;

/**
 * @author dergachevfv
 * @since 11/13/19
 */
@SuppressWarnings("checkstyle:magicnumber")
public class TreeBuilderTest {

    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String EXPECTED_JSON = getExpectedJSON();

    @Test
    public void test() throws JsonProcessingException {
        TestContext context = new TestContext();

        TestNode tree =
            node(c -> new TestNode("Node 1"))
                .child(
                    node(c -> new TestNode("Node 11"))
                        .children(c -> Arrays.asList(new ChildTestContext(1), new ChildTestContext(2)),
                            node(c -> new TestNode("Node 11 child " + c.i)))
                )
                .children(c -> Collections.emptyList(),
                    node(c -> {
                        throw new RuntimeException("unexpected Node 1 children");
                    }))
                .childOpt(c -> Optional.of(new TestContext()),
                    node(c -> new TestNode("Node 12")))
                .childOpt(c -> Optional.empty(),
                    node(c -> {
                        throw new RuntimeException("unexpected Node 1 childOpt");
                    }))
                .build(context);

        Assertions.assertThat(tree).isNotNull();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(tree);

        Assertions.assertThat(json).isNotNull();
        Assertions.assertThat(json).isEqualTo(EXPECTED_JSON);
    }

    private static class TestContext {
    }

    private static class ChildTestContext {
        private int i;

        ChildTestContext(int i) {
            this.i = i;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class TestNode implements TreeNode<TestNode> {
        private String title;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<TestNode> children = new ArrayList<>();

        TestNode(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public List<TestNode> getChildren() {
            return children;
        }

        @Override
        public void addChildren(List<TestNode> children) {
            this.children.addAll(children);
        }
    }

    private static String getExpectedJSON() {
        return "{" + LINE_SEPARATOR +
            "  \"title\" : \"Node 1\"," + LINE_SEPARATOR +
            "  \"children\" : [ {" + LINE_SEPARATOR +
            "    \"title\" : \"Node 11\"," + LINE_SEPARATOR +
            "    \"children\" : [ {" + LINE_SEPARATOR +
            "      \"title\" : \"Node 11 child 1\"" + LINE_SEPARATOR +
            "    }, {" + LINE_SEPARATOR +
            "      \"title\" : \"Node 11 child 2\"" + LINE_SEPARATOR +
            "    } ]" + LINE_SEPARATOR +
            "  }, {" + LINE_SEPARATOR +
            "    \"title\" : \"Node 12\"" + LINE_SEPARATOR +
            "  } ]" + LINE_SEPARATOR +
            "}";
    }
}
