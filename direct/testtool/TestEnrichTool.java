package ru.yandex.direct.internaltools.tools.testtool;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.internaltools.core.annotations.tool.AccessGroup;
import ru.yandex.direct.internaltools.core.annotations.tool.Action;
import ru.yandex.direct.internaltools.core.annotations.tool.Category;
import ru.yandex.direct.internaltools.core.annotations.tool.HideInProduction;
import ru.yandex.direct.internaltools.core.annotations.tool.Tool;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole;
import ru.yandex.direct.internaltools.core.enums.InternalToolAction;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.internaltools.core.implementations.MassInternalToolWithoutParam;
import ru.yandex.direct.internaltools.tools.testtool.container.TestEnrichToolInfo;

/**
 * Тестовый инструмент. Не выполняет никакой полезной работы, используется для проверки отображения результатов
 */
@HideInProduction
@Tool(
        name = "Тестовый инструмент для проверки Enrich'а",
        label = "test_enrich_tool",
        description = "Тестовый инструмент для проверки отображения результатов",
        consumes = InternalToolParameter.class,
        type = InternalToolType.REPORT
)
@Category(InternalToolCategory.OTHER)
@Action(InternalToolAction.REFRESH)
@AccessGroup({InternalToolAccessRole.SUPER, InternalToolAccessRole.DEVELOPER})
@ParametersAreNonnullByDefault
public class TestEnrichTool extends MassInternalToolWithoutParam<TestEnrichToolInfo> {

    @Override
    protected List<TestEnrichToolInfo> getMassData() {
        return Arrays.asList(
                new TestEnrichToolInfo(),
                new TestEnrichToolInfo()
                        .withBooleanValue(false)
        );
    }
}

