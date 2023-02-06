package ru.yandex.common.util.application;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class BasicMainTest {

    private static List<Class> initializedBy = new ArrayList<>();


    @Before
    public void setUp() {
        initializedBy.clear();
    }

    @After
    public void tearDown() {
        System.clearProperty(BasicMain.CONTEXT_INITIALIZERS_PROPERTY);
    }

    @Test
    public void initializeContextSingle() {
        System.setProperty(BasicMain.CONTEXT_INITIALIZERS_PROPERTY, MyInitializer.class.getName());
        BasicMain.initializeContext(new GenericApplicationContext());
        Assert.assertEquals(Collections.singletonList(MyInitializer.class), initializedBy);
    }


    @Test
    public void initializeContextNothing() {
        BasicMain.initializeContext(new GenericApplicationContext());
        Assert.assertEquals(Collections.emptyList(), initializedBy);
    }


    @Test
    public void initializeContextMulti() {
        System.setProperty(BasicMain.CONTEXT_INITIALIZERS_PROPERTY,
                MyInitializer.class.getName() + "," + MyAnotherIinitializer.class.getName());
        BasicMain.initializeContext(new GenericApplicationContext());
        Assert.assertEquals(Arrays.asList(MyInitializer.class, MyAnotherIinitializer.class), initializedBy);
    }

    public static class MyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            initializedBy.add(this.getClass());
        }
    }

    public static class MyAnotherIinitializer extends MyInitializer {
    }

}