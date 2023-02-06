package ru.yandex.travel.guice;

import com.google.inject.Scope;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class CustomScopes {

    public static final Scope THREAD = new ThreadLocalScope();

    private CustomScopes() {
    }

}