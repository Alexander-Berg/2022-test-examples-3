package ru.yandex.direct.web.entity.internaltools.service.testtool;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.internaltools.core.BaseInternalTool;
import ru.yandex.direct.internaltools.core.annotations.tool.AccessGroup;
import ru.yandex.direct.internaltools.core.annotations.tool.Action;
import ru.yandex.direct.internaltools.core.annotations.tool.Category;
import ru.yandex.direct.internaltools.core.annotations.tool.Tool;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole;
import ru.yandex.direct.internaltools.core.enums.InternalToolAction;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;


@Tool(
        name = "Тестовый инструмент",
        label = "_controller_test_tool",
        description = "Тестовый инструмент для проверки всего на свете",
        consumes = TestToolParameter.class,
        type = InternalToolType.REPORT
)
@Category(InternalToolCategory.OTHER)
@Action(InternalToolAction.REFRESH)
@AccessGroup({InternalToolAccessRole.MANAGER})
@ParametersAreNonnullByDefault
public class ControllerTestTool implements BaseInternalTool<TestToolParameter> {
    @Override
    public InternalToolResult process(TestToolParameter param) {
        InternalToolMassResult<TestToolResultItem> result = new InternalToolMassResult<>();
        result.setMessage("Your params list is here");

        result.addItem(new TestToolResultItem()
                .withKey("cb")
                .withValue(param.getCb().toString()));
        result.addItem(new TestToolResultItem()
                .withKey("text")
                .withValue(param.getText()));

        List<String> bytes = new ArrayList<>();
        if (param.getFileNotRequired() != null) {
            for (byte b : param.getFileNotRequired()) {
                bytes.add(String.valueOf(b));
            }
        }

        result.addItem(new TestToolResultItem()
                .withKey("fileNotRequired")
                .withValue(String.join(";", bytes)));
        return result;
    }
}
