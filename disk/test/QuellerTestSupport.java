package ru.yandex.chemodan.app.queller.test;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.chemodan.test.TestHelper;

/**
 * @author dbrylev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        QuellerTestsContextConfiguration.class,
})
@TestExecutionListeners(value = {
        DependencyInjectionTestExecutionListener.class,
})
public abstract class QuellerTestSupport {
    @Before
    public void init() {
        TestHelper.initialize();
    }
}
