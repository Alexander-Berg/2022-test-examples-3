package ru.yandex.market.stat.dicts.config;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.config.factory.JdbcTemplateProvider;
import ru.yandex.market.stat.dicts.config.integration_conf.DictionariesIntTestConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcLoadConfigFromFile;
import ru.yandex.market.stat.dicts.loaders.jdbc.SchemelessDictionaryRecord;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Ekaterina Lebedeva <kateleb@yandex-team.ru>
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DictionariesIntTestConfig.class)
@ActiveProfiles("integration-tests")
public class ConfigOkContextTest {

    @Autowired
    private JdbcConfig jdbcConfig;

    @Autowired
    private JdbcTemplateProvider jdbcTemplateProvider;

    @Test
    public void testLoadersCreated() throws IOException {
        //это тест будет падать при кривом конфиге:
        // - лишние отступы в  jdbc-dictionaries.yaml
        // - опечатки в полях jdbc-dictionaries.yaml (source -> source и тп)
        try {
            DictionaryLoadersHolder res = jdbcConfig.jdbcLoadersHolder();
            Assert.assertThat("Not enough loaders found!", res.getLoaders().size(), Matchers.greaterThan(100));
        } catch (IllegalStateException e) {
            Assert.fail("Failed to load ApplicationContext. Can't create loaders holder by jdbc config, check jdbc-dictionaries.yaml: " + e.getMessage());
        }
    }

    @Test
    public void testForDuplicates() throws IOException {
        // этот тест падает на дублирующихся именах словарей
        List<Dictionary<SchemelessDictionaryRecord>> allFutureTasks = jdbcConfig.readTasks().entrySet().stream().flatMap(e -> e.getValue().stream())
                .map(JdbcLoadConfigFromFile::flattenLoads).flatMap(Collection::stream).map(task -> Dictionary.from(task.getRelativePath(), SchemelessDictionaryRecord.class, task.getScale(), 1L, false)).collect(toList());
        Assert.assertThat("Not enough dictionaries found!", allFutureTasks.size(), Matchers.greaterThan(100));
        List<String> duplicateDicts = allFutureTasks.stream().map(Dictionary::nameForLoader).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet()
                .stream()
                .filter(p -> p.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toList());

        Assert.assertThat("Duplicate dictionaries found!!", duplicateDicts, Matchers.empty());
    }

    @Test
    public void testLoadersConfigFindsLoaderHolders() throws IOException {
        // этот тест падает на дублирующихся именах словарей, при создании бина
        try {
            new LoadersConfig().dictionaries(Collections.singletonList(jdbcConfig.jdbcLoadersHolder()));
        } catch (IllegalStateException e) {
            Assert.fail("Failed to load ApplicationContext. Can't create loaders holder by jdbc config, error: " + e.getMessage());
        }
    }
}
