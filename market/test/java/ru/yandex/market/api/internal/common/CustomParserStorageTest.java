package ru.yandex.market.api.internal.common;


import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.util.parser.Parser;

import javax.inject.Inject;
import java.util.Arrays;

public class CustomParserStorageTest extends ContainerTestBase {

    @Inject
    private CustomParserStorage storage;

    @Test
    public void canCreatePrivateParsersThroughtReflection() {
        Class<?> internalClass = Arrays.stream(TestComponentWithPrivateParser.class.getDeclaredClasses())
            .filter(x -> x.getSimpleName().equalsIgnoreCase("TestPrivateParser"))
            .findFirst().get();
        Parser parser = storage.get((Class<? extends Parser>) internalClass);
        Assert.assertNotNull(parser);
    }

    @Test
    public void canCreatePrivateParsersWithArgumentsThroughtReflection() {
        Class<?> internalClass = Arrays.stream(TestComponentWithPrivateParser.class.getDeclaredClasses())
            .filter(x -> x.getSimpleName().equalsIgnoreCase("TestPrivateParserWithoutDefaultCtor"))
            .findFirst().get();
        Parser parser = storage.get((Class<? extends Parser>) internalClass);
        Assert.assertNotNull(parser);

        TestComponentWithPrivateParser.TestComponentGetter p = (TestComponentWithPrivateParser.TestComponentGetter) parser;

        TestComponentWithPrivateParser.TestComponent testComponent = p.getTestComponent();
        Assert.assertEquals("bar", testComponent.foo());
    }

    @Test
    public void canCreatePublicParsersThroughSpring() {
        Class<?> internalClass = Arrays.stream(TestComponentWithPublicParser.class.getDeclaredClasses()).findFirst().get();
        Parser parser = storage.get((Class<? extends Parser>) internalClass);
        Assert.assertNotNull(parser);
    }
}

