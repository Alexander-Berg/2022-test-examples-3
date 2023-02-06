package ru.yandex.market.sqb.util;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbException;
import ru.yandex.market.sqb.exception.SqbInconsistentPackageException;
import ru.yandex.market.sqb.model.conf.QueryModel;
import ru.yandex.market.sqb.service.config.reader.PackageConfigurationReader;
import ru.yandex.market.sqb.service.config.reader.strategy.CompositeReaderStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_DIFFERENT_NAME;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_INSUFFICIENT_PARAMETER;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_NEGATIVE;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_POSITIVE;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.createReader;
import static ru.yandex.market.sqb.test.TestUtils.checkConstructor;
import static ru.yandex.market.sqb.util.SqbGenerationUtils.generateSQL;
import static ru.yandex.market.sqb.util.SqbGenerationUtils.generateSQLFromPackage;

/**
 * Unit-тесты для {@link SqbGenerationUtils}.
 *
 * @author Vladislav Bauer
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
class SqbGenerationUtilsTest {

    @Test
    void testConstructorContract() {
        checkConstructor(SqbGenerationUtils.class);
    }

    @Test
    void testReadQueryModelPositive() {
        final QueryModel queryModel = readQueryModel(FILE_POSITIVE);

        assertThat(queryModel, notNullValue());
    }

    @Test
    void testReadQueryModelNegative() {
        Assertions.assertThrows(SqbException.class, () -> readQueryModel(FILE_NEGATIVE));
    }

    @Test
    void testGenerateSQLByModelPositive() {
        final QueryModel queryModel = readQueryModel(FILE_POSITIVE);
        final String sql = generateSQL(queryModel);

        assertThat(sql, not(emptyOrNullString()));
    }

    @Test
    void testGenerateSQLByModelNegative() {
        Assertions.assertThrows(SqbException.class, () -> generateSQL(readQueryModel(FILE_NEGATIVE)));
    }

    @Test
    void testGenerateSQLByReaderPositive() {
        final String sql = generateSQL(createReader(FILE_POSITIVE));

        assertThat(sql, not(emptyOrNullString()));
    }

    @Test
    void testGenerateSQLByReaderNegative() {
        Assertions.assertThrows(SqbException.class, () -> generateSQL(createReader(FILE_NEGATIVE)));
    }

    @Test
    void testGenerateSQLFromPackageSingleConfiguration() {
        Supplier<String> reader = createReader(FILE_POSITIVE);
        CompositeReaderStrategy strategy = new CompositeReaderStrategy(reader);
        PackageConfigurationReader packageReader = new PackageConfigurationReader(strategy);
        String sql = generateSQLFromPackage(packageReader, true);

        assertThat(sql, not(emptyOrNullString()));
        assertThat(sql, not(containsString(" UNION ALL ")));
    }

    @Test
    void testGenerateSQLFromPackageTwoConfigurations() {
        String sql = runPackageSqlGeneration(FILE_POSITIVE, FILE_POSITIVE, true);

        assertThat(sql, not(emptyOrNullString()));
        assertThat(sql, containsString(" UNION ALL "));
    }

    @Test
    void testGenerateSQLFromPackageWithoutDuplicates() {
        String sql = runPackageSqlGeneration(FILE_POSITIVE, FILE_POSITIVE, false);

        assertThat(sql, not(emptyOrNullString()));
        assertThat(sql, containsString(" UNION "));
    }


    @Test
    void testGenerateSQLFromPackageDifferentParametersNumber() {
        Assertions.assertThrows(SqbInconsistentPackageException.class, () ->
                runPackageSqlGeneration(FILE_POSITIVE, FILE_INSUFFICIENT_PARAMETER, false));
    }


    @Test
    void testGenerateSQLFromPackageDifferentParameterName() {
        Assertions.assertThrows(SqbInconsistentPackageException.class, () ->
                runPackageSqlGeneration(FILE_POSITIVE, FILE_DIFFERENT_NAME, false));
    }


    private QueryModel readQueryModel(final String fileName) {
        final Supplier<String> configReader = createReader(fileName);
        return SqbGenerationUtils.readQueryModel(configReader);
    }

    private String runPackageSqlGeneration(String firstFile, String secondFile, boolean duplicates) {
        Supplier<String> firstReader = createReader(firstFile);
        Supplier<String> secondReader = createReader(secondFile);
        CompositeReaderStrategy strategy = new CompositeReaderStrategy(firstReader, secondReader);
        PackageConfigurationReader packageReader = new PackageConfigurationReader(strategy);
        return generateSQLFromPackage(packageReader, duplicates);
    }

}
