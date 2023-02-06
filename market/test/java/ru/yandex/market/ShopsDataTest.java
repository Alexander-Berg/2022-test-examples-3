package ru.yandex.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.sqb.service.config.ConfigurationReaderFactory;
import ru.yandex.market.sqb.test.YamlUtils;
import ru.yandex.market.sqb.test.db.DbQueryConfigChecker;
import ru.yandex.market.sqb.test.db.datasource.DbDataSourceHolder;

import static ru.yandex.market.DataUnitTestHelper.CONFIG_XML;
import static ru.yandex.market.DataUnitTestHelper.EXPECTED_FIELDS;
import static ru.yandex.market.DataUnitTestHelper.EXPECTED_RESULTS;
import static ru.yandex.market.DataUnitTestHelper.resource;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkAll;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkDefinedParameterTypes;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkParameters;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkRequiredFields;
import static ru.yandex.market.sqb.test.db.DbQueryConfigChecker.checkRows;

/**
 * Тесты для шопсдат.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ShopsDataTest extends FunctionalTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Тест direct_data")
    @DbUnitDataSet(before = "/direct_data/dataset.xml", type = DataSetType.FLAT_XML)
    void testDirectData() throws Exception {
        checkShopsData("direct_data", "DATAFEED_ID");
    }

    @Test
    @DisplayName("Тест eats_and_lavka_partners_data")
    @DbUnitDataSet(before = "/eats_and_lavka_partners_data/dataset.xml", type = DataSetType.FLAT_XML)
    void testEatsAndLavkaData() throws Exception {
        checkShopsData("eats_and_lavka_partners_data", "FEED_ID");
    }

    @Test
    @DisplayName("Тест fmcg_feeds_data")
    @DbUnitDataSet(before = "/fmcg_feeds_data/dataset.xml", type = DataSetType.FLAT_XML)
    void testFmcgFeedsData() throws Exception {
        checkShopsData("fmcg_feeds_data", "DATAFEED_ID");
    }

    @Test
    @DisplayName("Тест vendor_billing_data")
    @DbUnitDataSet(before = "/vendor_billing_data/dataset.xml", type = DataSetType.FLAT_XML)
    void testVendorBillingData() throws Exception {
        checkShopsData("vendor_billing_data", "SHOP_ID");
    }

    @Test
    @DisplayName("Тест suppliers_data")
    @DbUnitDataSet(before = "/suppliers_data/dataset.xml", type = DataSetType.FLAT_XML)
    void testSuppliersData() throws Exception {
        checkShopsData("suppliers_data", "DATAFEED_ID");
    }

    @Test
    @DisplayName("Тест shops_data")
    @DbUnitDataSet(before = "/shops_data/dataset.xml", type = DataSetType.FLAT_XML)
    void testShopsData() throws Exception {
        checkShopsData("shops_data", "DATAFEED_ID");
    }

    @Test
    @DisplayName("Тест foreign_shops_data")
    @DbUnitDataSet(before = "/foreign_shops_data/dataset.xml", type = DataSetType.FLAT_XML)
    void testForeignShopsData() throws Exception {
        checkShopsData("foreign_shops_data", "DATAFEED_ID");
    }

    private void checkShopsData(String configName, String uniqueKey) throws Exception {
        DbDataSourceHolder.runWithDataSource(dataSource, () -> {
            DataUnitTestHelper.verifyConfig(configName);
            Map<String, Map<String, Optional<String>>> definedParameterTypes = readDefinedParameterTypes();

            final Supplier<String> configReader = createConfigReader(configName);
            final DbQueryConfigChecker.QueryResultInfo resultInfo = checkAll(configReader);
            final List<DbQueryConfigChecker.ParamResultInfo> badParams = resultInfo.getBadParams();

            checkParameters(badParams);

            final List<Map<String, Object>> actualRows = resultInfo.getRows();
            final List<Map<String, Object>> expectedRows = YamlUtils.read(resource(configName, EXPECTED_RESULTS));

            checkDefinedParameterTypes(configReader, definedParameterTypes.get(configName));

            final boolean checkOrder = DbQueryConfigChecker.QueryResultInfo.isOrdered(resultInfo);
            checkRows(actualRows, expectedRows, uniqueKey, checkOrder);

            checkRequiredFields(resultInfo.getQuery(), resource(configName, EXPECTED_FIELDS));
        });
    }

    private Supplier<String> createConfigReader(final String configName) {
        return resource(configName, CONFIG_XML);
    }

    /**
     * Зачитывает прибитые типы параметров из definedParameterTypes.yaml.
     *
     * @return мапа вида: key -> имя конфига, value -> мапа имени параметра к его типу
     */
    private Map<String, Map<String, Optional<String>>> readDefinedParameterTypes() {
        Supplier<String> configurationReader =
                ConfigurationReaderFactory.createClasspathReader(ShopsDataTest.class, "/definedParameterTypes.yaml");
        List<Map<String, Object>> typeConfigs = YamlUtils.read(configurationReader);
        Map<String, Map<String, Optional<String>>> definedParameterTypes = new HashMap<>();

        typeConfigs.forEach(config ->
                definedParameterTypes.put(
                        String.valueOf(config.get("CONFIG")),
                        config.entrySet()
                                .stream()
                                .filter(e -> !e.getKey().equals("CONFIG"))
                                .collect(Collectors.toMap(
                                        e -> e.getKey().toUpperCase(),
                                        e -> Optional.of(e.getValue().toString())))));

        return definedParameterTypes;
    }
}
