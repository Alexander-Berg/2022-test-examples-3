package ru.yandex.market.api;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.api.integration.ContainerTestBase;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class AppPropertiesTest extends ContainerTestBase {
    @Inject
    private AppProperties properties;

    @Test
    public void enumerateKeys() {
        assertThat(properties.enumerateKeys().collect(Collectors.toList()), hasItems(startsWith("external."), startsWith("java.")));
    }
}