package ru.yandex.direct.internaltools.core.bootstrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.BaseInternalTool;
import ru.yandex.direct.internaltools.core.InternalToolProxy;
import ru.yandex.direct.internaltools.core.annotations.tool.Tool;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;

import static org.assertj.core.api.Assertions.assertThat;


public class InternalToolsRegistryGenerationTest {
    private static final String LABEL_ONE = "test_tool_one";
    private static final String LABEL_TWO = "test_tool_two";

    @Tool(name = "testToolOne", label = LABEL_ONE, description = "descr", type = InternalToolType.REPORT, consumes = InternalToolParameter.class)
    @ParametersAreNonnullByDefault
    private class TestToolOne implements BaseInternalTool<InternalToolParameter> {
        @Override
        public InternalToolResult process(InternalToolParameter parameter) {
            return null;
        }
    }

    @Tool(name = "testToolOne", label = LABEL_TWO, description = "descr", type = InternalToolType.WRITER, consumes = InternalToolParameter.class)
    @ParametersAreNonnullByDefault
    private class TestToolTwo implements BaseInternalTool<InternalToolParameter> {
        @Override
        public InternalToolResult process(InternalToolParameter parameter) {
            return null;
        }
    }

    @Tool(name = "testToolOne", label = LABEL_ONE, description = "descr", type = InternalToolType.REPORT, consumes = InternalToolParameter.class)
    @ParametersAreNonnullByDefault
    private class TestToolThree implements BaseInternalTool<InternalToolParameter> {
        @Override
        public InternalToolResult process(InternalToolParameter parameter) {
            return null;
        }
    }

    @Before
    public void before() {
    }

    @Test
    public void testToolsMapGeneration() {
        TestToolOne toolOne = new TestToolOne();
        TestToolTwo toolTwo = new TestToolTwo();

        Map<String, InternalToolProxy<?>> labelToProxy =
                InternalToolsRegistryBootstrap
                        .generateToolsMap(Arrays.asList(toolOne, toolTwo), null, Collections.emptyList(), 12);
        assertThat(labelToProxy)
                .containsOnlyKeys(LABEL_ONE, LABEL_TWO);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(labelToProxy.get(LABEL_ONE).getLabel()).isEqualTo(LABEL_ONE);
        soft.assertThat(labelToProxy.get(LABEL_TWO).getLabel()).isEqualTo(LABEL_TWO);
        soft.assertAll();
    }

    @Test(expected = InternalToolInitialisationException.class)
    public void testToolsMapGenerationError() {
        InternalToolsRegistryBootstrap
                .generateToolsMap(Arrays.asList(new TestToolOne(), new TestToolThree()), null, Collections.emptyList(),
                        12);
    }
}
