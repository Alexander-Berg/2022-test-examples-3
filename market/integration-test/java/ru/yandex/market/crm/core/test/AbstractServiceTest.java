package ru.yandex.market.crm.core.test;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mcrm.utils.test.StatefulHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ServicesTestConfig.class)
@TestPropertySource("/mcrm_core_test.properties")
public abstract class AbstractServiceTest {

    @Inject
    private ListableBeanFactory beanFactory;

    @Before
    public void setUp() {
        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::setUp);
    }

    @After
    public void tearDown() {
        beanFactory.getBeansOfType(StatefulHelper.class).values()
                .forEach(StatefulHelper::tearDown);
    }
}
