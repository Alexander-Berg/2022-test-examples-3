package ru.yandex.autotests.direct.utils.beans;

import java.util.List;

/**
 * User: xy6er
 * Date: 14.09.13
 * Time: 10:07
 */

public class SomeBean {
    private int intValue;
    private Integer integerValue;
    private Double doubleValue;
    private String stringValue;
    private Enum enumField;
    private SomeBean someBeanField;
    private String[] array;
    private List<String> list;

    public SomeBean() {
    }

    public SomeBean(String stringValue) {
        this.stringValue = stringValue;
    }

    public enum Enum {
        FIRST,
        SECOND,
        THIRD;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Enum getEnumField() {
        return enumField;
    }

    public void setEnumField(Enum enumField) {
        this.enumField = enumField;
    }

    public SomeBean getSomeBeanField() {
        return someBeanField;
    }

    public void setSomeBeanField(SomeBean someBeanField) {
        this.someBeanField = someBeanField;
    }

    public String[] getArray() {
        return array;
    }

    public void setArray(String[] array) {
        this.array = array;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }
}
