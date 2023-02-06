package ru.yandex.direct.operation.testing.entity;

import java.util.List;

import ru.yandex.direct.model.Model;

public class ComplexTextAdGroup implements Model {
    private List<Keyword> keywords;

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    public ComplexTextAdGroup withKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
        return this;
    }
}
