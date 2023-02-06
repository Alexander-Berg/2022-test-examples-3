package ru.yandex.autotests.direct.web.util.matchers;

/**
 * User: xy6er
 * Date: 14.09.13
 * Time: 10:07
 */

public class SomeBean {
    private String stringValue;
    private int intValue;
    private Integer integerValue;
    private Double doubleValue;
    private Enum enumField;
    private SomeClass someClassField;
    private String[] array;

    public enum Enum {
        FIRST,
        SECOND,
        THIRD;
    }

    public static class SomeClass {
        private String stringValue;

        public SomeClass(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Enum getEnumField() {
        return enumField;
    }

    public void setEnumField(Enum enumField) {
        this.enumField = enumField;
    }

    public SomeClass getSomeClassField() {
        return someClassField;
    }

    public void setSomeClassField(SomeClass someClassField) {
        this.someClassField = someClassField;
    }

    public String[] getArray() {
        return array;
    }

    public void setArray(String[] array) {
        this.array = array;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }
}
