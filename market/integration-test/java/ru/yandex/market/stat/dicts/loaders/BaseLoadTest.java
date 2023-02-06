package ru.yandex.market.stat.dicts.loaders;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Slf4j
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
public class BaseLoadTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Autowired
    protected List<DictionaryLoadersHolder> dictionaryLoadersHolders;
    protected List<DictionaryLoader> loaders;
    @Value("${dictionaries.to.test}")
    protected String dictionary;

    public static Object[][] makeTestData(Collection<String> dictionaries) {
        return dictionaries.stream()
                .map(dictionary -> new String[]{dictionary})
                .toArray(Object[][]::new);
    }

    @Before
    public void setUp() {
        loaders = dictionaryLoadersHolders.stream()
                .flatMap(holder -> holder.getLoaders().stream())
                .collect(toList());
    }

    protected DictionaryLoader<?> getLoader(String dict) {
        return loaders.stream()
                .filter(loader -> loader.getDictionary().nameForLoader().equals(dict))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dictionary not found: " + dict));
    }

    protected long loadDictionary() throws Exception {
        return loadDictionary(getDictionary());
    }

    protected long loadDictionary(String dict) throws Exception {
        return testLoadDict(dict, getLoader(dict), LocalDate.now().minusDays(1).atStartOfDay());
    }

    public static long testLoadDict(String dictionary, DictionaryLoader loader, LocalDateTime loadDateTime) throws Exception {

        LocalDateTime start = LocalDateTime.now();
        log.info("Loading {} for {} ...", dictionary, loadDateTime);
        long loadedRecords = loader.load(DEFAULT_CLUSTER, loadDateTime);
        log.info("Loaded {} records from {}", loadedRecords, dictionary);
        assertThat("Nothing loaded!", loadedRecords, greaterThan(0L));

        LocalDateTime end = LocalDateTime.now();
        log.info("Load took " + (Duration.between(start, end).toMillis() * 1.0 / 60000) +
                " minutes for " + loadedRecords + " records");
        return loadedRecords;
    }


    public String getDictionary() {
        return "";
    }
}
