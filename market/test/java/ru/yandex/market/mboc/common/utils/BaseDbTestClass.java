package ru.yandex.market.mboc.common.utils;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.datacamp.HashCalculator;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.ydb.TestYdbMockConfig;

/**
 * @author yuramalinov
 * @created 10.10.18
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = PGaaSZonkyInitializer.class,
    classes = {DbTestConfiguration.class, TestYdbMockConfig.class}
)
@Transactional
public abstract class BaseDbTestClass {
    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Resource(name = "slaveSqlNamedParameterJdbcTemplate")
    protected NamedParameterJdbcTemplate slaveNamedParameterJdbcTemplate;
    @Resource(name = "slaveSqlJdbcTemplate")
    protected JdbcTemplate slaveJdbcTemplate;
    @Autowired
    protected TransactionHelper transactionHelper;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected CategoryInfoRepository categoryInfoRepository;
    protected CategoryInfoCache categoryInfoCache;
    protected ContextedOfferDestinationCalculator offerDestinationCalculator;
    @Autowired
    protected StorageKeyValueService storageKeyValueService;
    protected HashCalculator hashCalculator;
    @Autowired
    protected DataCampIdentifiersService dataCampIdentifiersService;

    @Before
    public void baseSetup() {
        categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepository);
        hashCalculator = new HashCalculator(storageKeyValueService, dataCampIdentifiersService, categoryInfoCache);
        offerDestinationCalculator = new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService);
    }
}
