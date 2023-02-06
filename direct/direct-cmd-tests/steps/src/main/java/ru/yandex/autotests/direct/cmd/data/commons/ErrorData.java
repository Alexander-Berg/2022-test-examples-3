package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.ObjectErrors;

public class ErrorData {

    private String code;
    private String suffix;
    private String text;
    private String description;
    private String name;
    private String type;
    @SerializedName("object_errors")
    private ObjectErrors objectErrors;

    public ObjectErrors getObjectErrors() {
        return objectErrors;
    }

    public void setObjectErrors(ObjectErrors objectErrors) {
        this.objectErrors = objectErrors;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ErrorData withCode(String code) {
        this.code = code;
        return this;
    }

    public ErrorData withText(String text) {
        this.text = text;
        return this;
    }

    public ErrorData withDescription(String description) {
        this.description = description;
        return this;
    }

    public ErrorData withName(String name) {
        this.name = name;
        return this;
    }

    public ErrorData withType(String type) {
        this.type = type;
        return this;
    }
}
