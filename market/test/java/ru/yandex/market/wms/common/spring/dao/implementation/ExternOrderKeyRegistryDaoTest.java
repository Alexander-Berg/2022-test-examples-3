package ru.yandex.market.wms.common.spring.dao.implementation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class ExternOrderKeyRegistryDaoTest extends IntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;
    private ExternOrderKeyRegistryDao externOrderKeyRegistryDao;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2020-04-18T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    public void setupDao() {
        externOrderKeyRegistryDao = new ExternOrderKeyRegistryDao(jdbc, fixedClock);
    }

    @Test
    @DatabaseSetup("/db/dao/extern-order-key-registry/before.xml")
    @ExpectedDatabase(value = "/db/dao/extern-order-key-registry/insert-key.xml", assertionMode = NON_STRICT_UNORDERED)
    void insertOne() {
        externOrderKeyRegistryDao.insert("inserted-test-key");
    }

    @Test
    @DatabaseSetup("/db/dao/extern-order-key-registry/before.xml")
    void checkExists() {
        Assertions.assertTrue(
                externOrderKeyRegistryDao.exists("outbound-test-key"),
                "ExternOrder should exists, but it doesn't");
    }

    @Test
    @DatabaseSetup("/db/dao/extern-order-key-registry/before.xml")
    void checkNotExists() {
        Assertions.assertFalse(
                externOrderKeyRegistryDao.exists("inserted-test-key"),
                "ExternOrder shouldn't exists, but it does");
    }

    @Test
    @DatabaseSetup("/db/dao/extern-order-key-registry/before.xml")
    void throwExceptionOnDuplicateKey() {
        Assertions.assertThrows(DuplicateKeyException.class,
                () -> externOrderKeyRegistryDao.insert("outbound-test-key"));
    }

    @Test
    @DatabaseSetup("/db/dao/extern-order-key-registry/before.xml")
    void insertNullAuthor() {
        externOrderKeyRegistryDao.insert("inserted-test-key");
    }


}
