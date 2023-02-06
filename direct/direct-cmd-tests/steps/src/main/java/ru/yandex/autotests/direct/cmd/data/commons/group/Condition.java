package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Condition implements Comparable {

    @SerializedName("field")
    private String field;

    @SerializedName("relation")
    private String relation;

    @SerializedName("kind")
    private String kind;

    @SerializedName("type")
    private String type;

    @SerializedName("value")
    private Object value;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValueString() {
        if (value != null && String.class.isAssignableFrom(value.getClass())) {
            return (String) value;
        } else {
            return null;
        }
    }

    public Object getValue() {
        return value;
    }

    public List<String> getValueList() {
        if (value != null && List.class.isAssignableFrom(value.getClass())) {
            return (List<String>) value;
        } else {
            return null;
        }
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Condition withKind(String kind) {
        this.kind = kind;
        return this;
    }

    public Condition withType(String type) {
        this.type = type;
        return this;
    }

    public Condition withValue(Object value) {
        this.value = value;
        return this;
    }

    public Condition withField(String field) {
        this.field = field;
        return this;
    }

    public Condition withRelation(String relation) {
        this.relation = relation;
        return this;
    }

    @Override
    public int compareTo(Object o) {
        Condition c = (Condition) o;
        return getField().compareTo(c.getField());
    }
}
