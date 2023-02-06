package ru.yandex.market.stat.dicts.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import ru.yandex.market.stat.dicts.common.ConversionStrategy;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcLoadConfigFromFile;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;

@RunWith(Parameterized.class)
public class PropertiesValidationTest {

    private static final List<String> REQUIRED_JDBC_PROPS = Arrays.asList("jdbc.username", "jdbc.driver", "jdbc.url");
    private final static String JDBC_URL_KEY_POSTFIX = "jdbc.url";

    private final static String JDBC_URL_POSTGRES_PREFIX = "jdbc:postgresql";
    private final static String JDBC_URL_ORACLE_PREFIX = "jdbc:oracle";
    private final static String JDBC_URL_MYSQL_PREFIX = "jdbc:mysql";
    private final static String JDBC_URL_SQLSERVER_PREFIX = "jdbc:sqlserver";
    private final static String JDBC_URL_YQL_PREFIX = "jdbc:yql";
    private final static String JDBC_URL_CLICKHOUSE_PREFIX = "jdbc:clickhouse";

    private final static Map<String, Pattern> PREFIX_TO_PATTERN = new HashMap<>();

    private final static Set<String> VALID_POSTGRES_JDBC_EXAMPLES = Sets.newHashSet(
            "jdbc:postgresql://host1:1123,host.domain:4321,host.another.domain" +
                    ".2:4321/schema?param1=VaLue1&param2=Value2",
            "jdbc:postgresql://host1:1123/schema?param1=VaLue1&param2=Value2",
            "jdbc:postgresql://host1:1123,host2.domain:4321/schema",
            "jdbc:postgresql://host.domain.another.domain1:1123/schema"
    );

    private final static Set<String> INVALID_POSTGRES_JDBC_EXAMPLES = Sets.newHashSet(
            "",
            "owierngoiwenrg",
            "jdbc://host1:1123,host.domain:4321,host.another.domain.2:4321/schema?param1=VaLue1&param2=Value2",
            "werg:postgresql://host1:1123,host.domain:4321,host.another.domain" +
                    ".2:4321/schema?param1=VaLue1&param2=Value2",
            "jdbc:postgresql://host1:1123,,/schema?param1=VaLue1&param2=Value2",
            "jdbc:postgresql://host1:1123,,host.domain:4321/schema?param1=VaLue1&param2=Value2",
            "jdbc:postgresql://host1:1123,host.domain:4321/schema?",
            "jdbc:postgresql://host1:1123,host.domain:4321/schema?param1=value1?param2=value2"
    );

    private final static Pattern POSTGRES_JDBC_URL_PATTERN = Pattern.compile(
            "^jdbc:postgresql://" + // jdbc:dbms://
                    "([a-zA-Z\\-0-9.]+:[0-9]+,)*" + // host1:1111, - 0 или более раз с запятой
                    "([a-zA-Z\\-0-9.]+:[0-9]+)/" + // host:1111 - минимум 1 раз без запятой
                    "[a-zA-Z][a-zA-Z_0-9-]+" + // название БД
                    "(\\?[a-zA-Z\\-0-9=&${}.:/;]+)?$" // ?param1=value1 - параметры 0 или более, хотим один "?" и если
            // он есть,
            // то хоть какие-то параметры
    );

    private final static Pattern ORACLE_JDBC_URL_PATTERN = Pattern.compile(
            "^jdbc:oracle:" + // jdbc:dbms: - без двух слешей
                    "[a-z]+:@" + // thin: - idk what's this
                    "([a-zA-Z\\-0-9._]+(:[0-9]+)?,)*" + // host1:1111, - 0 или более раз с запятой, порта может не быть
                    "([a-zA-Z\\-0-9._]+(:[0-9]+)?)" + // host:1111 - минимум 1 раз без запятой, порта может не быть
                    "(/[a-zA-Z_0-9]+)*" + // название БД - может быть, а может не быть
                    "(\\?[a-zA-Z0-9=&${}.:/;]+)?$" // ?param1=value1 - параметры 0 или более, хотим один "?" и если
            // он есть,
            // то хоть какие-то параметры
    );

    private final static Pattern MYSQL_JDBC_URL_PATTERN = Pattern.compile(
            "^jdbc:mysql://" + // jdbc:dbms://
                    "([a-zA-Z\\-0-9.]+(:[0-9]+)?,)*" + // host1:1111, - 0 или более раз с запятой, порта может не быть
                    "([a-zA-Z\\-0-9.]+(:[0-9]+)?)/" + // host:1111 - минимум 1 раз без запятой, порта может не быть
                    "[a-zA-Z_]+" + // название БД
                    "(\\?[a-zA-Z0-9=&${}.:/;]+)?$" // ?param1=value1 - параметры 0 или более, хотим один "?" и если
            // он есть,
            // то хоть какие-то параметры
    );

    private final static Pattern SQLSERVER_JDBC_URL_PATTERN = Pattern.compile(
            "^jdbc:sqlserver://" + // jdbc:dbms://
                    "([a-zA-Z\\-0-9.]+:[0-9]+)" + // host:111 - минимум 1 раз без запятой
                    ";databaseName=[a-zA-Z_0-9]+" + // название БД
                    "(;encrypt=[a-zA-Z]+)?" + // нужно ли шифровать
                    "(;trustServerCertificate=[a-zA-Z]+)?" // нужно ли доверять самоподписанному сертификату от сервера
    );

    private final static Pattern CLICKHOUSE_JDBC_URL_PATTERN = Pattern.compile(
            "^jdbc:clickhouse://" +
                    "([a-zA-Z\\-0-9.]+:[0-9]+)" + // host1:1111
                    "[a-zA-Z_]+" + // название БД
                    "(\\?[a-zA-Z\\-0-9=&${}.:/;]+)?$" // ?param1=value1 несколько раз
    );

    private final static Pattern YQL_JDBC_URL_PATTERN = Pattern.compile(
            "^jdbc:yql://" + // jdbc:dbms://
                    "([a-zA-Z\\-0-9.]+:[0-9]+)"// host:1111 - минимум 1 раз без запятой
    );

    private final static Properties PRODUCTION_PROPERTIES;
    private final static Properties TESTING_PROPERTIES;
    private final static Properties BASE_PROPERTIES;

    static {
        PRODUCTION_PROPERTIES = readProperties("production");
        TESTING_PROPERTIES = readProperties("testing");
        BASE_PROPERTIES = readProperties(null);
        PREFIX_TO_PATTERN.put(JDBC_URL_POSTGRES_PREFIX, POSTGRES_JDBC_URL_PATTERN);
        PREFIX_TO_PATTERN.put(JDBC_URL_ORACLE_PREFIX, ORACLE_JDBC_URL_PATTERN);
        PREFIX_TO_PATTERN.put(JDBC_URL_MYSQL_PREFIX, MYSQL_JDBC_URL_PATTERN);
        PREFIX_TO_PATTERN.put(JDBC_URL_SQLSERVER_PREFIX, SQLSERVER_JDBC_URL_PATTERN);
        PREFIX_TO_PATTERN.put(JDBC_URL_YQL_PREFIX, YQL_JDBC_URL_PATTERN);
        PREFIX_TO_PATTERN.put(JDBC_URL_CLICKHOUSE_PREFIX, CLICKHOUSE_JDBC_URL_PATTERN);
    }

    private final String propertiesEnvironment;


    public PropertiesValidationTest(String propertiesEnvironment) {
        this.propertiesEnvironment = propertiesEnvironment;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"production"}, {"testing"}
        });
    }


    @Test
    public void validateValidJdbcUrlRegEx() {
        assertSoftly(softly ->
                VALID_POSTGRES_JDBC_EXAMPLES.forEach(v ->
                        matchesPattern(softly, POSTGRES_JDBC_URL_PATTERN, v)));
    }

    @Test
    public void invalidateInvalidJdbcUrlRegEx() {
        assertSoftly(softly ->
                INVALID_POSTGRES_JDBC_EXAMPLES.forEach(v ->
                        notMatchesPattern(softly, POSTGRES_JDBC_URL_PATTERN, v)));
    }

    @Test
    public void validatePostgresJdbcUrls() {
        performJdbcAssertions(
                getProperties(), JDBC_URL_POSTGRES_PREFIX, PREFIX_TO_PATTERN.get(JDBC_URL_POSTGRES_PREFIX)
        );
    }

    @Test
    public void validateOracleJdbcUrls() {
        performJdbcAssertions(getProperties(), JDBC_URL_ORACLE_PREFIX, PREFIX_TO_PATTERN.get(JDBC_URL_ORACLE_PREFIX));
    }

    @Test
    public void validateMysqlJdbcUrls() {
        performJdbcAssertions(getProperties(), JDBC_URL_MYSQL_PREFIX, PREFIX_TO_PATTERN.get(JDBC_URL_MYSQL_PREFIX));
    }

    @Test
    public void validateSqlserverJdbcUrls() {
        performJdbcAssertions(
                getProperties(), JDBC_URL_SQLSERVER_PREFIX, PREFIX_TO_PATTERN.get(JDBC_URL_SQLSERVER_PREFIX)
        );
    }

    @Test
    public void validateYqlJdbcUrls() {
        performJdbcAssertions(
                getProperties(), JDBC_URL_YQL_PREFIX, PREFIX_TO_PATTERN.get(JDBC_URL_YQL_PREFIX)
        );
    }

    @Test
    public void validateNoUnknownPrefixesExist() {
        Properties properties = getProperties();
        Set<String> prefixesFromProperties = properties.keySet().stream()
                .filter(key -> ((String) key).endsWith(JDBC_URL_KEY_POSTFIX))
                .map(key -> properties.getProperty((String) key))
                .filter(prop -> !(prop.startsWith("${") && prop.endsWith("}")))
                .map(this::cutJdbcDbmsPrefixFromJdbcUrl)
                .collect(Collectors.toSet());
        Set<String> knownPrefixes = PREFIX_TO_PATTERN.keySet();

        assertSoftly(softly -> softly.assertThat(knownPrefixes).containsAll(prefixesFromProperties));
    }

    @Test
    public void validateEverySourceHasProps() throws IOException {
        Properties properties = getProperties();
        Set<String> jdbcSources = readJdbcSources();
        for (String source : jdbcSources) {
            checkPropsFor(source, properties);
        }
    }

    private void checkPropsFor(String source, Properties properties) {
        String prefix = "dictionaries.loaders." + source + ".";
        Set<String> props = new HashSet<>();
        for (Properties propfile : Arrays.asList(BASE_PROPERTIES, properties)) {
            props.addAll(propfile.keySet().stream()
                    .filter(key -> ((String) key).startsWith(prefix))
                    .map(key -> ((String) key).replace(prefix, ""))
                    .collect(Collectors.toList()));
        }
        String errorMsg = format("Source %s is not configured for %s! No required properties found: %s",
                source, propertiesEnvironment,
                REQUIRED_JDBC_PROPS.stream().map(f -> prefix + f).collect(joining(", ")));
        Assert.assertThat(errorMsg, REQUIRED_JDBC_PROPS, everyItem(isIn(props)));
    }

    private Set<String> readJdbcSources() throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        try (InputStream inputStream = resourceLoader.getResource("configs/jdbc-dictionaries.yaml").getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, Charsets.UTF_8)) {
            InjectableValues inject = new InjectableValues.Std()
                    .addValue(ConversionStrategy.class, ConversionStrategy.STANDARD);
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
            ObjectReader objectReader = mapper.reader(inject).forType(new TypeReference<Map<String, List<JdbcLoadConfigFromFile>>>() {
            });
            Map<String, List<JdbcLoadConfigFromFile>> result = objectReader.readValue(reader);
            return result.keySet();
        }
    }

    private Properties getProperties() {
        if ("production".equals(propertiesEnvironment)) {
            return PRODUCTION_PROPERTIES;
        }
        if ("testing".equals(propertiesEnvironment)) {
            return TESTING_PROPERTIES;
        }
        throw new RuntimeException(
                format("Test is broken! received %s environment, but have no idea what is it", propertiesEnvironment)
        );
    }

    private void performJdbcAssertions(Properties properties, String prefix, Pattern pattern) {
        assertSoftly(softly -> properties.keySet().stream()
                .filter(key -> ((String) key).endsWith(JDBC_URL_KEY_POSTFIX))
                .map(key -> properties.getProperty((String) key))
                .filter(v -> hasPrefix(prefix, v))
                .forEach(v -> matchesPattern(softly, pattern, v)));
    }

    private boolean hasPrefix(String prefix, String value) {
        return value.startsWith(prefix);
    }

    private void matchesPattern(SoftAssertions softly, Pattern pattern, String value) {
        softly.assertThat(value).matches(pattern);
    }

    private void notMatchesPattern(SoftAssertions softly, Pattern pattern, String value) {
        softly.assertThat(value).doesNotMatch(pattern);
    }

    private String cutJdbcDbmsPrefixFromJdbcUrl(String url) {
        String[] parts = url.split(":");
        return format("%s:%s", parts[0], parts[1]);
    }

    @SneakyThrows
    private static Properties readProperties(String environment) {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        String path = environment == null ? "dictionaries-yt.properties" :
                format("%s/dictionaries-yt-%s.properties", environment, environment);
        bean.setLocation(new ClassPathResource(path));
        bean.afterPropertiesSet();
        return bean.getObject();
    }

}
