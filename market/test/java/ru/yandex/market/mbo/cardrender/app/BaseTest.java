package ru.yandex.market.mbo.cardrender.app;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbo.cardrender.app.config.DbConfig;
import ru.yandex.market.mbo.cardrender.app.config.PgInitializer;
import ru.yandex.market.mbo.cardrender.app.config.RenderKeyValueConfig;
import ru.yandex.market.mbo.cardrender.app.config.RepositoryConfig;
import ru.yandex.market.mbo.cardrender.app.config.SaasPushConfig;
import ru.yandex.market.mbo.cardrender.app.config.YtRenderPathConfig;


/**
 * @author apluhin
 * @created 6/21/21
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = PgInitializer.class,
        classes = {

                DbConfig.class,
                RenderKeyValueConfig.class,
                SaasPushConfig.class,
                RepositoryConfig.class,
                YtRenderPathConfig.class
        }
)
@Transactional
public abstract class BaseTest {


}
