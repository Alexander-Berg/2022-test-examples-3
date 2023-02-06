package ru.yandex.chemodan.app.djfs.core.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.chemodan.app.djfs.core.db.DaoProxyFactory;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.MongoDjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.PgDjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.operations.OperationDao;
import ru.yandex.chemodan.app.djfs.core.operations.PgOperationDao;

/**
 * Перекрывает нужные бины по имени
 *
 * @author eoshch
 */
@Configuration
public class RandomFailuresContextConfiguration {
    @Bean
    public RandomFailingInvocationHandler.ProbabilitySource randomFailuresProbabilitySource() {
        return new RandomFailingInvocationHandler.ProbabilitySource();
    }

    @Bean
    @Primary
    public DjfsResourceDao djfsResourceDaoProxy(DaoProxyFactory daoProxyFactory, MongoDjfsResourceDao mongoDao,
            PgDjfsResourceDao pgDao, RandomFailingInvocationHandler.ProbabilitySource probabilitySource)
    {
        DjfsResourceDao instance = daoProxyFactory.create(DjfsResourceDao.class, pgDao);
        return RandomFailingInvocationHandler.proxy(DjfsResourceDao.class, instance, probabilitySource);
    }

    @Bean
    @Primary
    public OperationDao operationDaoProxy(DaoProxyFactory daoProxyFactory,
            PgOperationDao pgDao, RandomFailingInvocationHandler.ProbabilitySource probabilitySource)
    {
        OperationDao instance = daoProxyFactory.create(OperationDao.class, pgDao);
        return RandomFailingInvocationHandler.proxy(OperationDao.class, instance, probabilitySource);
    }
}
