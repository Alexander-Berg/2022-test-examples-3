package ru.yandex.travel.webdriver;

import org.junit.rules.ExternalResource;

import javax.inject.Inject;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class WebDriverResource extends ExternalResource {

    @Inject
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        driverManager.startDriver();
    }

    protected void after() {
        driverManager.stopDriver();
    }
}
