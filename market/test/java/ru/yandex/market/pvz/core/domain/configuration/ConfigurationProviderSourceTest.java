package ru.yandex.market.pvz.core.domain.configuration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobal;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalQueryService;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.CONFIGURATION_CACHE_VERSION;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ConfigurationProviderSourceTest {

    private final ConfigurationGlobalRepository configurationGlobalRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @SpyBean
    private ConfigurationProviderSource configurationProviderSource;

    @SpyBean
    private ConfigurationGlobalQueryService configurationGlobalQueryService;

    @Test
    void testChangeCacheVersionReloadsAfterCheck() {
        configurationProviderSource.changeCacheVersion();
        configurationProviderSource.checkReloadRequired();

        verify(configurationProviderSource, atLeastOnce()).reloadCache();
    }

    @Test
    void testCheckReloadReloadsCacheIfVersionChanged() {
        String version = randomAlphanumeric(8);
        String newVersion = randomAlphanumeric(10);
        configurationGlobalRepository.save(new ConfigurationGlobal(CONFIGURATION_CACHE_VERSION, version));
        configurationProviderSource.checkReloadRequired();

        clearInvocations(configurationProviderSource);
        configurationGlobalRepository.save(new ConfigurationGlobal(CONFIGURATION_CACHE_VERSION, newVersion));

        verify(configurationProviderSource, never()).reloadCache();
        configurationProviderSource.checkReloadRequired();
        verify(configurationProviderSource, times(1)).reloadCache();
    }

    @Test
    void testCheckReloadRequiredDoesNothingIfCacheTheSame() {
        configurationProviderSource.changeCacheVersion();
        clearInvocations(configurationGlobalQueryService);

        configurationProviderSource.checkReloadRequired();
        configurationProviderSource.checkReloadRequired();
        configurationProviderSource.checkReloadRequired();

        verify(configurationGlobalQueryService, never()).getConfigurationMap();
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void testDoesNotFailOnEmptyVersion(String version) {
        if (version != null) {
            configurationGlobalRepository.save(new ConfigurationGlobal(CONFIGURATION_CACHE_VERSION, version));
        }
        configurationProviderSource.checkReloadRequired();
        configurationProviderSource.changeCacheVersion();

        String savedVersion = configurationGlobalRepository.findById(CONFIGURATION_CACHE_VERSION)
                .map(ConfigurationGlobal::getValue)
                .orElseThrow();
        String cachedVersion = configurationProviderSource.getGlobal()
                .getValue(CONFIGURATION_CACHE_VERSION)
                .orElseThrow();
        assertThat(cachedVersion).isEqualTo(savedVersion).isNotEmpty();
    }

    @Test
    void testCacheReloadsInPlace() {
        String key = randomAlphanumeric(10);
        ConfigurationProvider global = configurationProviderSource.getGlobal();
        assertThat(global.getValue(key)).isEmpty();

        String value = randomAlphanumeric(10);
        configurationGlobalCommandService.setValue(key, value);
        configurationProviderSource.reloadCache();

        assertThat(global.getValue(key).orElseThrow()).isEqualTo(value);
    }
}
