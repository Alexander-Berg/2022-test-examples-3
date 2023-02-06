package ru.yandex.direct.internaltools.tools.testtool;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.internaltools.core.BaseInternalTool;
import ru.yandex.direct.internaltools.core.InternalToolProxy;
import ru.yandex.direct.internaltools.core.annotations.tool.AccessGroup;
import ru.yandex.direct.internaltools.core.annotations.tool.Category;
import ru.yandex.direct.internaltools.core.annotations.tool.Disclaimers;
import ru.yandex.direct.internaltools.core.annotations.tool.HideInProduction;
import ru.yandex.direct.internaltools.core.annotations.tool.Tool;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.internaltools.tools.testtool.container.TestToolResultInfo;
import ru.yandex.direct.internaltools.tools.testtool.model.TestToolParameter;


/**
 * Тестовый инструмент. Не выполняет никакой полезной работы, используется для проверки автогенерации интерфейса
 */
@HideInProduction
@Tool(
        name = "Тестовый инструмент",
        label = "test_tool",
        description = "Тестовый инструмент для проверки всего на свете",
        consumes = TestToolParameter.class,
        type = InternalToolType.WRITER
)
@Disclaimers({"Этот отчет ничего не делает", "Данные за 1703 год отсутствуют"})
@Category(InternalToolCategory.OTHER)
@AccessGroup({InternalToolAccessRole.SUPER, InternalToolAccessRole.DEVELOPER})
@ParametersAreNonnullByDefault
public class TestTool implements BaseInternalTool<TestToolParameter> {
    @Override
    public InternalToolResult process(TestToolParameter param) {
        InternalToolMassResult<TestToolResultInfo> result = new InternalToolMassResult<>();
        result.setMessage("Your params list");

        result.addItem(new TestToolResultInfo()
                .withKey("hiddenValue")
                .withValue(param.getHiddenValue().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("cb")
                .withValue(param.getCb().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("cbt")
                .withValue(String.valueOf(param.isCbt())));
        result.addItem(new TestToolResultInfo()
                .withKey("number")
                .withValue(param.getNumber().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("numberLimits")
                .withValue(param.getNumberLimits().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("numberId")
                .withValue(param.getNumberId() == null ? "null" : param.getNumberId().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("shard")
                .withValue(String.valueOf(param.getShard())));
        result.addItem(new TestToolResultInfo()
                .withKey("text")
                .withValue(param.getText()));
        result.addItem(new TestToolResultInfo()
                .withKey("textArea")
                .withValue(param.getTextArea()));
        result.addItem(new TestToolResultInfo()
                .withKey("textSelect")
                .withValue(param.getTextSelect()));
        result.addItem(new TestToolResultInfo()
                .withKey("stringMultipleSelect")
                .withValue(param.getStringMultipleSelect().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("enumMultipleSelect")
                .withValue(param.getEnumMultipleSelect().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("longMultipleSelect")
                .withValue(param.getLongMultipleSelect() == null ? "null" : param.getLongMultipleSelect().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("textWithLen")
                .withValue(param.getTextWithLen()));
        result.addItem(new TestToolResultInfo()
                .withKey("date")
                .withValue(param.getDate().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("dateFixed")
                .withValue(param.getDateFixed().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("dateToday")
                .withValue(param.getDateToday().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("dateTime")
                .withValue(param.getDateTime().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("dateTimeFixed")
                .withValue(param.getDateTimeFixed().toString()));
        result.addItem(new TestToolResultInfo()
                .withKey("dateTimeNow")
                .withValue(param.getDateTimeNow().toString()));

        List<String> fileBytes = new ArrayList<>();
        if (param.getFile() != null) {
            for (byte b : param.getFile()) {
                fileBytes.add(String.valueOf(b));
            }
        }
        result.addItem(new TestToolResultInfo()
                .withKey("file")
                .withValue(String.join(";", fileBytes)));

        List<String> fileNotReqBytes = new ArrayList<>();
        if (param.getFileNotRequired() != null) {
            for (byte b : param.getFileNotRequired()) {
                fileNotReqBytes.add(String.valueOf(b));
            }
        }
        result.addItem(new TestToolResultInfo()
                .withKey("fileNotRequired")
                .withValue(String.join(";", fileNotReqBytes)));
        return result;
    }

    @Override
    public InternalToolProxy.Builder<TestToolParameter> preCreate(
            InternalToolProxy.Builder<TestToolParameter> builder) {
        List<String> disclaimers = new ArrayList<>(builder.getDisclaimers());
        disclaimers
                .add(String.format("Этот пункт динамически добавлен при генерации %s",
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
        return builder
                .withDisclaimers(disclaimers);
    }
}
