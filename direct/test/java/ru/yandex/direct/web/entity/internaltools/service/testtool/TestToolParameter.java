package ru.yandex.direct.web.entity.internaltools.service.testtool;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ru.yandex.direct.internaltools.core.annotations.input.CheckBox;
import ru.yandex.direct.internaltools.core.annotations.input.File;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.Text;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class TestToolParameter extends InternalToolParameter {
    @Input(label = "Галочка")
    @CheckBox(checked = true)
    private Boolean cb;

    @Input(label = "Текст", description = "Описание", required = false)
    @Text(valueMaxLen = 20)
    private String text;

    @Input(label = "Необязательный файл", required = false)
    @File
    private byte[] fileNotRequired;

    public Boolean getCb() {
        return cb;
    }

    public void setCb(Boolean cb) {
        this.cb = cb;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public byte[] getFileNotRequired() {
        return fileNotRequired;
    }

    public void setFileNotRequired(byte[] fileNotRequired) {
        this.fileNotRequired = fileNotRequired;
    }
}
