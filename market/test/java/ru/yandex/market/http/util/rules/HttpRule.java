package ru.yandex.market.http.util.rules;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.rules.ExternalResource;

/**
 * @author dimkarp93
 */
public class HttpRule extends ExternalResource {
    private Collection<AutoCloseable> permanentResources = new ArrayList<>();
    private Collection<AutoCloseable> resources = new ArrayList<>();

    public HttpRule addPermanent(AutoCloseable resource) {
        permanentResources.add(resource);
        return this;
    }

    public HttpRule add(AutoCloseable resource) {
        resources.add(resource);
        return this;
    }

    @Override
    protected void after() {
        permanentResources.forEach(this::safeClose);
        resources.forEach(this::safeClose);
        resources.clear();
        super.after();
    }


    private void safeClose(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {

        }
    }
}
