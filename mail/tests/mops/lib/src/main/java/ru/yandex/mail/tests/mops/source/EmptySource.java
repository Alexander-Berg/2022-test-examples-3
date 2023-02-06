package ru.yandex.mail.tests.mops.source;

public class EmptySource implements Source {
    @Override
    public <T> void fill(T obj) throws Exception {
    }
}