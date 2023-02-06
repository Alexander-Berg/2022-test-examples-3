package ru.yandex.market.stat.dicts.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.stat.dicts.config.YtDictionaryConfig;
import ru.yandex.market.stat.dicts.integration.conf.PropertiesDictionariesUTestConfig;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.yt.YtService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.stat.yt.YtAttributes.SCHEMA;
import static ru.yandex.market.stat.yt.YtAttributes.TYPE;

@Slf4j
@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@ContextConfiguration(classes = {PropertiesDictionariesUTestConfig.class, YtDictionaryConfig.class})
public class SchemeITest {

    public static final YPath PRODUCTION_DICT_DIR = YPath.simple("//home/market/production/mstat/dictionaries");
    public static final YPath PRESTABLE_DICT_DIR = YPath.simple("//home/market/prestable/mstat/dictionaries");
    public static final YPath TESTING_DICT_DIR = YPath.simple("//home/market/testing/mstat/dictionaries/" +
        System.getProperty("user.name"));

    public static final Set<String> KNOWN_ISSUES_DICTS = ImmutableSet.of(
        "urlchecker_price" // empty in testing db
    );

    public static final Set<String> IGNORE_DICTS = ImmutableSet.of(
        "count_offers_by_param_names",
        "categories_and_parameters",
        "copy_states",
        "incoming",
        "work",
        "rev_shares",
        "analyst_orders_dict",
        "shop_category",
        "dynamic_pricing",
        "distr_partners",
        "analyst_shops_dict",
        "market_clicks_ch",
        "distribution",  // todo in production one times in month
        "clch_cluster",  // todo prestable (нет данных с 2018-03-15)
        "market_clicks_vendors_ch",  // todo prestable (нет данных вообще)
        "market_cpa_clicks_vendors_ch",  // todo prestable (нет данных вообще)
        "vendor_models",  // todo prestable (нет данных с 2017-06-25)
        "conversion_rates"
    );

    @Autowired
    private Yt yt;

    @Ignore("Manual check")
    @Test
    public void comparePrestableWithProduction() {
        String yesterday = LocalDate.now().minusDays(1).toString();

        Map<String, Option<YTreeNode>> productionSchemas = getScheme(PRODUCTION_DICT_DIR, yesterday);
        Map<String, Option<YTreeNode>> prestableSchemas = getScheme(PRESTABLE_DICT_DIR, yesterday);

        Set<String> productionNotFound = new HashSet<>(prestableSchemas.keySet());
        productionNotFound.removeAll(productionSchemas.keySet());
        log.warn("In production dictionaries not found: {}", productionNotFound);

        Set<String> prestableNotFound = new HashSet<>(productionSchemas.keySet());
        prestableNotFound.removeAll(prestableSchemas.keySet());
        log.warn("In prestable dictionaries not found: {}", prestableNotFound);

        List<String> badDicts = productionSchemas.entrySet().stream()
            .filter(e -> !KNOWN_ISSUES_DICTS.contains(e.getKey()))
            .filter(e -> {
                String dict = e.getKey();
                Option<YTreeNode> productionSchema = e.getValue();
                Option<YTreeNode> prestableSchema = prestableSchemas.get(dict);
                return badPrestableSchema(dict, productionSchema, prestableSchema);
            })
            .map(Map.Entry::getKey)
            .collect(toList());

        assertThat(badDicts, Matchers.emptyIterable());
    }

    @Ignore("Manual check")
    @Test
    public void testTestingWithProduction() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        String dictionary = "cpa_orders";

        Option<YTreeNode> productionSchema = getSchema(PRODUCTION_DICT_DIR, dictionary, yesterday);
        Option<YTreeNode> testingSchema = getSchema(TESTING_DICT_DIR, dictionary, yesterday);

        boolean result = badPrestableSchema(dictionary, productionSchema, testingSchema);

        assertThat(result, equalTo(false));
    }

    @Ignore("need for manual check")
    @Test
    public void readFirstLines() {
        String yesterday = LocalDate.now().minusDays(1).toString();

        yt.cypress().list(PRESTABLE_DICT_DIR, Cf.set(TYPE))
            .stream()
            .filter(node -> {
                String type = node.getAttribute(TYPE)
                    .orElse(YTree.stringNode(YtService.YtNodeType.FILE))
                    .stringValue();
                return type.equals(YtService.YtNodeType.DIRECTORY);
            })
            .map(YTreeNode::stringValue)
            .filter(dict -> !IGNORE_DICTS.contains(dict))
            .forEach(dict ->
                yt.tables().read(
                    PRESTABLE_DICT_DIR.child(dict).child(yesterday).withRange(0, 1),
                    YTableEntryTypes.JACKSON_UTF8,
                    (Consumer<JsonNode>) node -> System.out.println("{\"" + dict + "\": " + node.toString() + "},")
                )
            );
    }

    private boolean badPrestableSchema(String dict, Option<YTreeNode> productionSchema, Option<YTreeNode> prestableSchema) {
        if (productionSchema.isPresent() != prestableSchema.isPresent()) {
            return true;
        }
        List<YTreeNode> prod = productionSchema.get().asList();
        List<YTreeNode> prest = prestableSchema.get().asList();

        Map<String, YTreeNode> prestFields = prest.stream()
            .collect(Collectors.toMap(e -> e.asMap().get("name").stringValue(), e -> e));

        boolean result = false;
        for (YTreeNode prodField : prod) {
            String prodFieldName = prodField.asMap().get("name").stringValue();
            YTreeNode prestField = prestFields.get(prodFieldName);
            if (prestField == null) {
                log.warn("Bad dict {} field {} not found", dict, prodFieldName);
                result = true;
                continue;
            }

            String prodFieldType = prodField.asMap().get("type").stringValue();
            String prestFieldType = prestField.asMap().get("type").stringValue();
            if (!prodFieldType.equals(prestFieldType)) {
                log.warn("Bad dict {} field {} has differ type (prod: {}, prest: {})",
                    dict, prodFieldName, prodFieldType, prestFieldType);
                result = true;
                continue;
            }

            if (!prodField.equals(prestField)) {
                log.warn("Bad dict {} differ fields (prod: {}, prest: {})", dict, prodField, prestField);
                result = true;
            }
        }

        return result;
    }

    private Map<String, Option<YTreeNode>> getScheme(YPath path, String day) {
        return yt.cypress().list(path, Cf.set(TYPE))
            .stream()
            .filter(node -> {
                String type = node.getAttribute(TYPE)
                    .orElse(YTree.stringNode(YtService.YtNodeType.FILE))
                    .stringValue();
                return type.equals(YtService.YtNodeType.DIRECTORY);
            })
            .map(YTreeNode::stringValue)
            .filter(dict -> !IGNORE_DICTS.contains(dict))
            .collect(toMap(
                Function.identity(),
                dict -> getSchema(path, dict, day)
            ));
    }

    private Option<YTreeNode> getSchema(YPath path, String dict, String day) {
        return Option.wrap(yt.cypress().get(path.child(dict).child(day), Cf.set(SCHEMA)).getAttribute(SCHEMA));
    }
}
