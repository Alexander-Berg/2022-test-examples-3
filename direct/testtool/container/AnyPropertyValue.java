package ru.yandex.direct.internaltools.tools.testtool.container;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.Select;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.tools.testtool.preprocessors.SetAnyPropertyToolPreProcessor;

@ParametersAreNonnullByDefault
public class AnyPropertyValue extends InternalToolParameter {
    @Input(label = "Имя проперти", processors = SetAnyPropertyToolPreProcessor.class)
    @Select(preprocessed = true)
    private String name;

    @Input(label = "Новое значение")
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
