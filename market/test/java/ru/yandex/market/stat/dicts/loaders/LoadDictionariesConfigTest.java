package ru.yandex.market.stat.dicts.loaders;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import ru.yandex.market.stat.dicts.config.JdbcConfig;
import ru.yandex.market.stat.dicts.config.factory.DataSourceProvider;
import ru.yandex.market.stat.dicts.config.factory.JdbcTemplateProvider;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.services.DictionaryStorage;
import ru.yandex.market.stat.dicts.services.DictionaryYtService;
import ru.yandex.market.stat.yt.YtClusterProvider;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@TestPropertySource({
        "classpath:contextualized-dictionaries-yt-testing.properties"
})
@Import({
        JdbcConfig.class
        })
@TestExecutionListeners({
        MockitoTestExecutionListener.class,
        ResetMocksTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
})

public class LoadDictionariesConfigTest {

    @MockBean
    public DictionaryStorage ytDictionaryStorage;

    @MockBean
    public DictionaryYtService ytService;

    @MockBean
    public YtClusterProvider ytClusterProvider;

    @MockBean
    public DataSourceProvider dataSourceProvider;

    @MockBean
    public JdbcTemplateProvider jdbcTemplateProvider;

    @Autowired
    public List<DictionaryLoadersHolder> holders;

    /**
     * Поднимает конфигурацию лоадеров и сравнивает их количество с суммой количества конфигов с source: и sql:.
     * destination у некоторых конфигов не указывается, отсюда эвристика про сумму, некоторые лоадеры у нас особенные
     * и не указываются в конфиге, поэтому используется сравнение isGreaterThanOrEqualTo.
     */
    @Test
    public void happyLoadOfADictionariesConfig() {
        String configString = loadResourceAsString("configs/jdbc-dictionaries.yaml");
        int countOfSourceConfigs = StringUtils.countOccurrencesOf(configString, "source:");
        int countOfSqlConfigs = StringUtils.countOccurrencesOf(configString, "sql:");
        assertThat(holders).isNotEmpty().withFailMessage("No loader holders found, check JdbcConfig class");
        long loadersCount = holders.stream().mapToLong(h -> h.getLoaders().size()).sum();
        int lowerConfigsBound = countOfSourceConfigs + countOfSqlConfigs;
        assertThat(loadersCount)
                .withFailMessage(format(
                        "Configuration has found %d loaders, while config file has at least %d of them," +
                                " check jdbc-dictionaries.yaml", loadersCount, lowerConfigsBound)
                )
                .isGreaterThanOrEqualTo(lowerConfigsBound);

    }

    @SneakyThrows
    protected static String loadResourceAsString(String which) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(which);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}

