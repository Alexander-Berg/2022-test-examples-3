package ru.yandex.direct.env;

import javax.annotation.concurrent.NotThreadSafe;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.env.EnvironmentType.DEVELOPMENT;
import static ru.yandex.direct.env.EnvironmentType.PRODUCTION;
import static ru.yandex.direct.env.EnvironmentTypeReader.PROPERTY_NAME;

@NotThreadSafe
public class EnvironmentTest {
    @Rule
    public final ProvideSystemProperty propRule = new ProvideSystemProperty(PROPERTY_NAME, DEVELOPMENT.toString());

    @Before
    public void setUp() throws Exception {
        Environment.clearCache();
    }

    @After
    public void tearDown() throws Exception {
        Environment.clearCache();
    }


    @Test
    public void current_DevelopmentFromProperties() throws Exception {
        assertThat(Environment.getCached(), Matchers.is(DEVELOPMENT));
    }

    @Test
    public void current_SecondCall_Cached() throws Exception {
        Environment.getCached();

        String oldProp = System.getProperty(PROPERTY_NAME);
        try {
            System.setProperty(PROPERTY_NAME, PRODUCTION.toString());
            assertThat(Environment.getCached(), Matchers.is(DEVELOPMENT));
        } finally {
            System.setProperty(PROPERTY_NAME, oldProp);
        }
    }
}
