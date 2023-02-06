package ru.yandex.chemodan.app.djfs.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

/**
 * @author eoshch
 */
@ContextConfiguration(classes = {
        EventManagerContextConfiguration.class,
        EventManagerTest.TestConfiguration.class,
})
public class EventManagerTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static class TestEvent implements EventManager.Event {
    }

    private static class ChildTestEvent extends TestEvent {
    }

    public static class TestEventHandler {
        @EventManager.EventHandler
        public void handle(TestEvent event) {
            handled += 1;
        }
    }

    public static class TestChildEventHandler {
        @EventManager.EventHandler
        public void handle(ChildTestEvent event) {
            handled += 1;
        }
    }

    private static int handled;

    @Configuration
    public static class TestConfiguration {
        @Bean
        public TestEventHandler testEventHandler() {
            return new TestEventHandler();
        }

        @Bean
        public TestChildEventHandler testChildEventHandler() {
            return new TestChildEventHandler();
        }
    }

    @Autowired
    public EventManager eventManager;

    @Before
    public void before() {
        handled = 0;
    }

    @Test
    public void sendTestEvent() {
        eventManager.send(new TestEvent());
        Assert.assertEquals(1, handled);
    }

    @Test
    public void sendChildTestEvent() {
        eventManager.send(new ChildTestEvent());
        Assert.assertEquals(2, handled);
    }
}
