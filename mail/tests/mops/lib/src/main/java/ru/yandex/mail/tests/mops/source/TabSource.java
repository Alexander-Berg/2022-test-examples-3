package ru.yandex.mail.tests.mops.source;

import lombok.val;

public class TabSource implements Source {
    private final String tab;

    public TabSource(String tab) {
        this.tab = tab;
    }

    @Override
    public <T> void fill(T obj) throws Exception {
        val method = obj.getClass().getMethod("withTab", String.class);
        method.invoke(obj, tab);
    }
}