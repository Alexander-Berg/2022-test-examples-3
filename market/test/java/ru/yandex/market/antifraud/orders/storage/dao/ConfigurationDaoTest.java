package ru.yandex.market.antifraud.orders.storage.dao;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigurationEntity;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigEnum.ANTIFRAUD_OFFLINE_BAN_USER;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConfigurationDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private ConfigurationDao configurationDao;

    @Before
    public void init() {
        configurationDao = new ConfigurationDao(jdbcTemplate);
    }

    @Test
    public void getConfiguration() {
        ConfigurationEntity e1 = configurationDao.save(ConfigurationEntity.builder()
                .parameter(ANTIFRAUD_OFFLINE_BAN_USER)
                .updatedAt(Instant.now())
                .config(AntifraudJsonUtil.toJsonTree(Boolean.TRUE))
                .build());
        assertThat(configurationDao.getConfiguration(ANTIFRAUD_OFFLINE_BAN_USER).get()).isEqualTo(e1);
    }


}
