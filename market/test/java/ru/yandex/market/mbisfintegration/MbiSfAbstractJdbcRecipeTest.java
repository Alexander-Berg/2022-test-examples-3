package ru.yandex.market.mbisfintegration;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;
import ru.yandex.market.mbisfintegration.salesforce.SalesForceTestConfiguration;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 09.02.2022
 */
//@TestPropertySource("classpath:95_application_test.properties")
@ContextConfiguration(classes = SalesForceTestConfiguration.class)
public abstract class MbiSfAbstractJdbcRecipeTest extends AbstractJdbcRecipeTest {
}
