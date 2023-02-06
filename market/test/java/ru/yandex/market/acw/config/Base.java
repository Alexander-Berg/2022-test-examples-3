package ru.yandex.market.acw.config;

import org.jooq.Configuration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.acw.db.daos.ExtendedCwYtProcessedTablesDao;
import ru.yandex.market.acw.db.daos.ExtendedImageCacheDao;
import ru.yandex.market.acw.db.daos.ExtendedImageQueueDao;
import ru.yandex.market.acw.db.daos.ExtendedTextCacheDao;
import ru.yandex.market.acw.db.daos.ExtendedTextQueueDao;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class,
        classes = CommonTestConfig.class)
@Transactional
public abstract class Base {

    @Autowired
    @Qualifier("jooq.config.configuration")
    protected Configuration configuration;
    @Autowired
    @Qualifier("jooq.dao.text")
    protected ExtendedTextCacheDao textCacheDao;
    @Autowired
    @Qualifier("jooq.dao.image")
    protected ExtendedImageCacheDao imageCacheDao;
    @Autowired
    protected ExtendedTextQueueDao textQueueDao;
    @Autowired
    protected ExtendedImageQueueDao imageQueueDao;
    @Autowired
    protected ExtendedCwYtProcessedTablesDao extendedCwYtProcessedTablesDao;
}
