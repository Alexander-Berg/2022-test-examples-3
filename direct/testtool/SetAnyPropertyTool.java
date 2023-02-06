package ru.yandex.direct.internaltools.tools.testtool;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.internaltools.core.annotations.tool.AccessGroup;
import ru.yandex.direct.internaltools.core.annotations.tool.Action;
import ru.yandex.direct.internaltools.core.annotations.tool.Category;
import ru.yandex.direct.internaltools.core.annotations.tool.Disclaimers;
import ru.yandex.direct.internaltools.core.annotations.tool.HideInProduction;
import ru.yandex.direct.internaltools.core.annotations.tool.Tool;
import ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole;
import ru.yandex.direct.internaltools.core.enums.InternalToolAction;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.internaltools.core.implementations.MassInternalTool;
import ru.yandex.direct.internaltools.tools.ppcproperties.container.PropertyInfo;
import ru.yandex.direct.internaltools.tools.testtool.container.AnyPropertyValue;

@Tool(
        name = "Неограниченное редактирование ppc_property",
        label = "set_any_ppc_property",
        description = "С помощью этого отчета можно отредактировать любую запись в ppc_properties.\n"
                + "Отчет доступен только на тестовых средах.\n\n"
                + "Отчет предназначен для тестирования возможности динамического управления полями ввода",
        consumes = AnyPropertyValue.class,
        type = InternalToolType.WRITER
)
@Action(InternalToolAction.SET)
@Category(InternalToolCategory.OTHER)
@AccessGroup({InternalToolAccessRole.SUPER, InternalToolAccessRole.DEVELOPER})
@Disclaimers({"Даже на тестовой среде поменяв проперти можно кому-то что-то сломать. Пользуйтесь ответственно"})
@HideInProduction
@ParametersAreNonnullByDefault
public class SetAnyPropertyTool extends MassInternalTool<AnyPropertyValue, PropertyInfo> {
    private final PpcPropertiesSupport propertiesSupport;

    @Autowired
    public SetAnyPropertyTool(PpcPropertiesSupport propertiesSupport) {
        this.propertiesSupport = propertiesSupport;
    }

    @Override
    protected List<PropertyInfo> getMassData() {
        return propertiesSupport.getAllProperties()
                .entrySet().stream()
                .map(e -> new PropertyInfo(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PropertyInfo::getName))
                .collect(Collectors.toList());
    }

    @Override
    protected List<PropertyInfo> getMassData(AnyPropertyValue param) {
        propertiesSupport.set(param.getName(), param.getValue());
        return getMassData();
    }
}
