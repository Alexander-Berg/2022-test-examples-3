package ru.yandex.direct.operation.testing.entity;


public class TextAdGroup extends AdGroup {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TextAdGroup withName(String name) {
        this.name = name;
        return this;
    }
}
