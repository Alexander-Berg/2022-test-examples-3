package ru.yandex.direct.internaltools.core.bootstrap;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.internaltools.core.BaseInternalTool;
import ru.yandex.direct.internaltools.core.InternalToolProxy;
import ru.yandex.direct.internaltools.core.annotations.input.File;
import ru.yandex.direct.internaltools.core.annotations.input.Group;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.tool.AccessGroup;
import ru.yandex.direct.internaltools.core.annotations.tool.Action;
import ru.yandex.direct.internaltools.core.annotations.tool.Category;
import ru.yandex.direct.internaltools.core.annotations.tool.Disclaimers;
import ru.yandex.direct.internaltools.core.annotations.tool.Tool;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.core.enrich.InternalToolEnrichProcessorFactory;
import ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole;
import ru.yandex.direct.internaltools.core.enums.InternalToolAction;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;
import ru.yandex.direct.internaltools.core.input.InternalToolInputGroup;

import static org.assertj.core.api.Assertions.assertThat;

public class InternalToolProxyGenerationTest {
    public class TestToolParameter extends InternalToolParameter {
        @Input(label = "Дата")
        public LocalDate date;
        @Input(label = "Строка", required = false)
        public String string;
        @Group(name = "Группа", priority = 10)
        @Input(label = "Число", required = false)
        public Long aLong;
    }

    @Tool(
            name = "Тестовый отчет",
            label = "test_tool",
            description = "Тестовый отчет для проверки всего",
            consumes = TestToolParameter.class,
            type = InternalToolType.WRITER,
            includeChart = true
    )
    @Category(InternalToolCategory.BS_EXPORT)
    @AccessGroup({InternalToolAccessRole.MANAGER, InternalToolAccessRole.PLACER})
    @Disclaimers({"предупреждение 1", "предупреждение 2"})
    @Action(InternalToolAction.REFRESH)
    @ParametersAreNonnullByDefault
    public class TestTool implements BaseInternalTool<TestToolParameter> {
        boolean preCreateCalled = false;

        @Override
        public InternalToolResult process(TestToolParameter testToolParameter) {
            return new InternalToolResult()
                    .withMessage(testToolParameter.date.toString());
        }

        @Override
        public InternalToolProxy.Builder<TestToolParameter> preCreate(
                InternalToolProxy.Builder<TestToolParameter> builder) {
            preCreateCalled = true;
            return builder;
        }
    }

    @Tool(
            name = "Тестовый отчет",
            label = "test_tool",
            description = "Тестовый отчет для проверки всего",
            consumes = TestToolParameter.class,
            type = InternalToolType.WRITER
    )
    @ParametersAreNonnullByDefault
    public class TestToolDefaults implements BaseInternalTool<TestToolParameter> {
        @Override
        public InternalToolResult process(TestToolParameter testToolParameter) {
            return new InternalToolResult()
                    .withMessage(testToolParameter.date.toString());
        }
    }

    public class TestToolParameterWithFiles extends InternalToolParameter {
        @Input(label = "Дата")
        public LocalDate date;
        @File
        @Input(label = "File One")
        public byte[] fileOne;
        @File
        @Input(label = "File Two")
        public byte[] fileTwo;
    }

    @Tool(
            name = "Тестовый отчет",
            label = "test_tool",
            description = "Тестовый отчет для проверки всего",
            consumes = TestToolParameterWithFiles.class,
            type = InternalToolType.WRITER
    )
    @ParametersAreNonnullByDefault
    public class TestToolWithFiles implements BaseInternalTool<TestToolParameterWithFiles> {
        @Override
        public InternalToolResult process(TestToolParameterWithFiles testToolParameter) {
            return new InternalToolResult()
                    .withMessage(testToolParameter.date.toString());
        }
    }

    @ParametersAreNonnullByDefault
    public class TestToolNoAnnotations implements BaseInternalTool<TestToolParameter> {
        @Override
        public InternalToolResult process(TestToolParameter testToolParameter) {
            return new InternalToolResult()
                    .withMessage(testToolParameter.date.toString());
        }
    }

    @Tool(
            name = "Тестовый отчет",
            label = "Test tool а-",
            description = "Тестовый отчет для проверки всего",
            consumes = TestToolParameter.class,
            type = InternalToolType.WRITER
    )
    @ParametersAreNonnullByDefault
    public class TestToolBadLabel implements BaseInternalTool<TestToolParameter> {
        @Override
        public InternalToolResult process(TestToolParameter testToolParameter) {
            return new InternalToolResult()
                    .withMessage(testToolParameter.date.toString());
        }
    }

    @Mock
    private InternalToolEnrichProcessorFactory enrichProcessorFactory;

    @Test
    public void testParseToolDescription() {
        TestTool tool = new TestTool();
        InternalToolProxy<TestToolParameter> toolProxy =
                InternalToolProxyBootstrap.proxyFromTools(tool, enrichProcessorFactory, Collections.emptyList(), 7);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(toolProxy.getName())
                .isEqualTo("Тестовый отчет");
        soft.assertThat(toolProxy.getLabel())
                .isEqualTo("test_tool");
        soft.assertThat(toolProxy.getDescription())
                .isEqualTo("Тестовый отчет для проверки всего");
        soft.assertThat(toolProxy.getType())
                .isEqualTo(InternalToolType.WRITER);
        soft.assertThat(toolProxy.getCategory())
                .isEqualTo(InternalToolCategory.BS_EXPORT);
        soft.assertThat(toolProxy.getAction())
                .isEqualTo(InternalToolAction.REFRESH);
        soft.assertThat(toolProxy.getDisclaimers())
                .containsExactly("предупреждение 1", "предупреждение 2");
        soft.assertThat(toolProxy.getAllowedRoles())
                .containsExactlyInAnyOrder(InternalToolAccessRole.SUPER, InternalToolAccessRole.MANAGER,
                        InternalToolAccessRole.PLACER);
        soft.assertThat(toolProxy.writesData())
                .isTrue();
        soft.assertThat(toolProxy.getInternalTool())
                .isEqualTo(tool);
        soft.assertThat(tool.preCreateCalled)
                .isTrue();
        soft.assertAll();
    }

    @Test
    public void testParseToolDescriptionDefaults() {
        BaseInternalTool<TestToolParameter> tool = new TestToolDefaults();
        InternalToolProxy<TestToolParameter> toolProxy =
                InternalToolProxyBootstrap.proxyFromTools(tool, enrichProcessorFactory, Collections.emptyList(), 7);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(toolProxy.getName())
                .isEqualTo("Тестовый отчет");
        soft.assertThat(toolProxy.getLabel())
                .isEqualTo("test_tool");
        soft.assertThat(toolProxy.getDescription())
                .isEqualTo("Тестовый отчет для проверки всего");
        soft.assertThat(toolProxy.getType())
                .isEqualTo(InternalToolType.WRITER);
        soft.assertThat(toolProxy.getCategory())
                .isEqualTo(InternalToolCategory.OTHER);
        soft.assertThat(toolProxy.getAction())
                .isEqualTo(InternalToolAction.SHOW);
        soft.assertThat(toolProxy.getDisclaimers())
                .isEmpty();
        soft.assertThat(toolProxy.getAllowedRoles())
                .containsExactlyInAnyOrder(InternalToolAccessRole.SUPER, InternalToolAccessRole.SUPERREADER,
                        InternalToolAccessRole.DEVELOPER);
        soft.assertThat(toolProxy.writesData())
                .isTrue();
        soft.assertThat(toolProxy.getInternalTool())
                .isEqualTo(tool);
        soft.assertThat(toolProxy.isAcceptsFiles())
                .isFalse();
        soft.assertThat(toolProxy.getFileFieldsNames())
                .isEmpty();
        soft.assertAll();
    }

    @Test
    public void testParseToolDescriptionWithFile() {
        InternalToolProxy<TestToolParameterWithFiles> toolProxy =
                InternalToolProxyBootstrap
                        .proxyFromTools(new TestToolWithFiles(), enrichProcessorFactory, Collections.emptyList(), 7);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(toolProxy.isAcceptsFiles())
                .isTrue();
        soft.assertThat(toolProxy.getFileFieldsNames())
                .containsExactlyInAnyOrder("fileOne", "fileTwo");
        soft.assertAll();
    }

    @Test
    public void testParseToolInputGroups() {
        InternalToolProxy<TestToolParameter> toolProxy =
                InternalToolProxyBootstrap
                        .proxyFromTools(new TestTool(), enrichProcessorFactory, Collections.emptyList(), 7);

        List<InternalToolInputGroup<TestToolParameter>> inputGroups = toolProxy.getInputGroups();
        assertThat(inputGroups)
                .size().isEqualTo(2);
        assertThat(inputGroups.get(0).getName())
                .isEqualTo("Группа");
        assertThat(inputGroups.get(1).getName())
                .isEmpty();
        assertThat(inputGroups.get(0).getInputList())
                .size().isEqualTo(1);
        assertThat(inputGroups.get(0).getInputList().get(0).getName())
                .isEqualTo("aLong");
        assertThat(inputGroups.get(1).getInputList())
                .size().isEqualTo(2);
        assertThat(inputGroups.get(1).getInputList().get(0).getName())
                .isEqualTo("date");
        assertThat(inputGroups.get(1).getInputList().get(1).getName())
                .isEqualTo("string");
    }

    @Test(expected = InternalToolInitialisationException.class)
    public void testParseToolNoAnnotationError() {
        InternalToolProxyBootstrap
                .proxyFromTools(new TestToolNoAnnotations(), enrichProcessorFactory, Collections.emptyList(), 7);
    }

    @Test(expected = InternalToolInitialisationException.class)
    public void testParseToolBadLabel() {
        InternalToolProxyBootstrap
                .proxyFromTools(new TestToolBadLabel(), enrichProcessorFactory, Collections.emptyList(), 7);
    }

    @Test
    public void testExpandRoles() {
        Set<InternalToolAccessRole> roles = EnumSet.of(InternalToolAccessRole.SUPERREADER);

        InternalToolProxyBootstrap.expandRoles(roles);

        assertThat(roles)
                .containsOnly(
                        InternalToolAccessRole.SUPERREADER,
                        InternalToolAccessRole.DEVELOPER,
                        InternalToolAccessRole.SUPER)
                .as("Расширили роли ожидаемо");
    }

    @Test
    public void testExpandRolesFull() {
        Set<InternalToolAccessRole> roles = EnumSet.of(InternalToolAccessRole.INTERNAL_USER);

        InternalToolProxyBootstrap.expandRoles(roles);

        assertThat(roles)
                .containsOnly(InternalToolAccessRole.values())
                .as("Расширили роли ожидаемо");
    }
}
