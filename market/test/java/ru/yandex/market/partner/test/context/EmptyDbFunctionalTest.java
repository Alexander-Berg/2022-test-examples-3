package ru.yandex.market.partner.test.context;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.guava.ForgetfulSuppliersInitializer;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.tanker.dao.CachedTankerDaoImpl;
import ru.yandex.market.core.test.context.AllowSpiesInitializer;
import ru.yandex.market.core.test.context.TestPropertiesInitializer;

/**
 * Базовый класс для функциональных тестов mbi-partner.
 * Тесты, которые хотят сами управлять содержимым БД могут наследоваться отсюда, а не от FunctionalTest.
 *
 * @author Vladislav Bauer
 */
@ActiveProfiles("functionalTest")
@SpringJUnitConfig(
        locations = "classpath:/ru/yandex/market/partner/test/context/functional-test-config.xml",
        initializers = {
                TestPropertiesInitializer.class,
                FunctionalTestPropertiesInitializer.class,
                AllowSpiesInitializer.class,
                ForgetfulSuppliersInitializer.class
        }
)
@PropertySources({
        @PropertySource("classpath*:common/common-servant.properties"),
        @PropertySource("classpath*:common-servant.properties"),
        @PropertySource("classpath:ru/yandex/market/partner/test/context/functional-test-config.properties")
})
public abstract class EmptyDbFunctionalTest extends JupiterDbUnitTest {

    @Autowired
    @Qualifier("baseUrl")
    protected String baseUrl;

    @Autowired
    private CachedTankerDaoImpl tankerDao;

    @AfterEach
    void cleanUpCaches() {
        tankerDao.cleanUpCache();
    }

}
