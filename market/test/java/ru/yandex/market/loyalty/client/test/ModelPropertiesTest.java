package ru.yandex.market.loyalty.client.test;

import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.loyalty.test.SourceScanner;
import ru.yandex.market.loyalty.test.TestUtils;

public class ModelPropertiesTest {
    @Test
    public void fieldGettersAndSettersNamesTest() {
        var brokenFields = SourceScanner.findAllClasses("ru.yandex.market.loyalty.client.model")
                .filter(clazz -> !clazz.getName().contains("Builder")
                        && !clazz.getName().contains("Test")
                        && !clazz.isEnum()
                )
                .flatMap(clazz -> TestUtils.checkClassPropertyNamesIsRight(clazz).stream())
                .collect(Collectors.joining("\n"));

        Assert.assertTrue("These fields may produce (de)serialization bugs:\n" + brokenFields,
                brokenFields.isEmpty());
    }
}
