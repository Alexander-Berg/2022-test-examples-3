package ru.yandex.market.promoboss.postgres;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;
import ru.yandex.market.promoboss.dao.ConfigurationDao;

@ContextConfiguration(classes = {ConfigurationDao.class})
public class ConfigurationDaoTest extends AbstractJdbcRecipeTest {

    private static final String PROPERTY_NAME = "test property";
    private static final Boolean PROPERTY_BOOLEAN_VALUE = true;

    @Autowired
    private ConfigurationDao configurationDao;

    @Test
    public void shouldSaveAndReturnProperties() {

        configurationDao.set(PROPERTY_NAME, PROPERTY_BOOLEAN_VALUE);
        Optional<Boolean> res = configurationDao.get(PROPERTY_NAME, Boolean.TYPE);
        Assertions.assertTrue(res.isPresent());
        Assertions.assertTrue(res.get());
    }
}
