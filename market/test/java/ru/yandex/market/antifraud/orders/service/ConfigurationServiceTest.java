package ru.yandex.market.antifraud.orders.service;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.storage.dao.ConfigurationDao;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigEnum;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigurationEntity;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationServiceTest {

    @Mock
    private ConfigurationDao configurationDao;
    private ConfigurationService configurationService;

    @Before
    public void init() {
        configurationService = new ConfigurationService(configurationDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailCheckParameter() {
        configurationService.save(ConfigurationEntity.builder()
                .parameter(ConfigEnum.ANTIFRAUD_OFFLINE_CANCEL_ORDER)
                .config(AntifraudJsonUtil.toJsonTree("{\"a\":12}"))
                .build());
    }

    @Test
    public void useCache() {
        when(configurationDao.getConfiguration(eq(ConfigEnum.ANTIFRAUD_OFFLINE_BAN_USER)))
                .thenReturn(Optional.of(ConfigurationEntity.builder()
                        .parameter(ConfigEnum.ANTIFRAUD_OFFLINE_BAN_USER)
                        .config(AntifraudJsonUtil.toJsonTree("True"))
                        .build()));
        assertThat(configurationService.userBanEnabled()).isTrue();
        assertThat(configurationService.userBanEnabled()).isTrue();
        verify(configurationDao, times(1)).getConfiguration(eq(ConfigEnum.ANTIFRAUD_OFFLINE_BAN_USER));
    }


    @Test
    public void checkValidate() {
        String json = "{\"parameter\": \"HIDE_MARKERS\", \"config\": [\"reseller\", \"blue\"]}";
        ConfigurationEntity entity = AntifraudJsonUtil.fromJson(json, ConfigurationEntity.class);
        configurationService.save(entity); // should not throw ex
    }

}
