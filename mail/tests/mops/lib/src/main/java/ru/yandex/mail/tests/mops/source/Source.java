package ru.yandex.mail.tests.mops.source;

public interface Source {
    <T> void fill(T obj) throws Exception;
}
