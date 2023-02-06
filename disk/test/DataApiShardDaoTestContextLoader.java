package ru.yandex.chemodan.app.dataapi.core.dao.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ContextLoader;

import ru.yandex.chemodan.app.dataapi.test.DataApiTestSupport;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;

/**
 * @author tolmalev
 */
public class DataApiShardDaoTestContextLoader implements ContextLoader,
        ApplicationContextInitializer<ConfigurableApplicationContext>
{

    @Override
    public String[] processLocations(Class<?> clazz, String... locations) {
        return new String[] {};
    }

    @Override
    public ApplicationContext loadContext(String... locations) throws Exception {
        DataApiTestSupport.initProperties();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().addActiveProfile(ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG);
        context.getEnvironment().addActiveProfile(ActivateEmbeddedPg.EMBEDDED_PG);
        context.register(JdbcShardDaoTestsContextConfiguration.class);
        context.refresh();
        return context;
    }

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

    }
}
