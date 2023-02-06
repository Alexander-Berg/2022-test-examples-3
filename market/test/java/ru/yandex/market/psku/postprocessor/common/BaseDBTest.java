package ru.yandex.market.psku.postprocessor.common;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.psku.postprocessor.common.config.TestDataBaseConfiguration;
import ru.yandex.market.psku.postprocessor.common.db.config.CommonDaoConfig;
import ru.yandex.market.psku.postprocessor.common.db.dao.SessionDao;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class,
    classes = {
        TestDataBaseConfiguration.class,
        CommonDaoConfig.class
})
@Transactional
public abstract class BaseDBTest {

    @Autowired
    protected Configuration jooqConfiguration;
    @Autowired
    protected SessionDao sessionDao;

    protected DSLContext dsl() {
        return DSL.using(jooqConfiguration);
    }

    protected Long createNewSession(String sessionName) {
        return sessionDao.createNewSession(sessionName);
    }
}
