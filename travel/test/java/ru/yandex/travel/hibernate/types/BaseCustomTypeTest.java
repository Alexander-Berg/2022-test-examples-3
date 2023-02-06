package ru.yandex.travel.hibernate.types;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;

import java.util.List;

public abstract class BaseCustomTypeTest {
    protected SessionFactory sessionFactory;

    @Before
    public void before() {

        Configuration configuration = new Configuration(
                new BootstrapServiceRegistryBuilder()
                        .applyClassLoader(getClass().getClassLoader())
                        .build()
        );
        for (Class clazz : getAnnotatedClasses()) {
            configuration.addAnnotatedClass(clazz);
        }
        configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem:testdb");
        configuration.setProperty("hibernate.dialect", "ru.yandex.travel.hibernate.dialects.CustomH2Dialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create");
        sessionFactory = configuration.buildSessionFactory();
    }

    @After
    public void after() {
        sessionFactory.close();
    }

    protected abstract List<Class> getAnnotatedClasses();

}
