package ru.yandex.market.abo.core.hiding.rules.common.export;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.IndexerRules;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.Model;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.ModelExceptShopRule;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.Offer;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.ShopRule;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.StopWord;
import ru.yandex.market.abo.core.hiding.rules.common.export.json.Vendor;

@ExtendWith(MockitoExtension.class)
public class DumperTest {
    private static final String EMPTY_INDEXER_RULES_JSON = loadFileToString("/hiding/export/empty_indexer_rules.json");
    private static final String FULL_INDEXER_RULES_JSON = loadFileToString("/hiding/export/full_indexer_rules.json");

    @Spy
    @InjectMocks
    private OfferHidingRulesExportService exportService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void dumpFullIndexerRules() throws IOException, JSONException {
        IndexerRules rules = createFullIndexerRules();
        testDumpIndexerRules(rules, FULL_INDEXER_RULES_JSON);
    }


    @Test
    public void dumpEmptyIndexerRules() throws IOException, JSONException {
        testDumpIndexerRules(new IndexerRules(), EMPTY_INDEXER_RULES_JSON);
    }

    @Test
    public void fullIndexerRulesMustConformToSchema() throws Exception {
        testSchemaConformance(createFullIndexerRules());
    }

    @Test
    public void emptyIndexerRulesMustConformToSchema() throws Exception {
        testSchemaConformance(new IndexerRules());
    }

    private void testSchemaConformance(IndexerRules rules) throws JSONException, IOException {
        JSONObject rawSchema = new JSONObject(loadFileToString(OfferHidingRulesExporter.OFFERS_HIDING_SCHEMA_LOCAL_PATH));
        Schema schema = SchemaLoader.load(rawSchema);
        schema.validate(new JSONObject(dumpIndexerRules(rules)));
    }

    private static IndexerRules createFullIndexerRules() {
        IndexerRules rules = new IndexerRules();
        rules.setModelRules(Arrays.asList(
                new ModelExceptShopRule(10L, new LinkedHashSet<>(Arrays.asList(1L, 2L))),
                new ModelExceptShopRule(20L, Collections.emptySet())
        ));

        ShopRule shopRule1 = new ShopRule(1);
        shopRule1.addIncludes(Arrays.asList(new Model(11), new Model(12),
                new Offer("/offer11.html"), new Offer("/offer12.html"),
                new Vendor(111), new Vendor(112),
                new StopWord("word11"), new StopWord("word12")));

        ShopRule shopRule2 = new ShopRule(2);
        shopRule2.addIncludes(Arrays.asList(new Model(20),
                new Offer("/offer20.html"),
                new Vendor(222),
                new StopWord("word20")));


        rules.setShopRules(Arrays.asList(shopRule1, shopRule2));

        return rules;
    }

    private void testDumpIndexerRules(IndexerRules rules, String expectedJson) throws IOException, JSONException {
        String actualJson = dumpIndexerRules(rules);
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    private String dumpIndexerRules(IndexerRules rules) throws IOException {
        Mockito.doReturn(rules).when(exportService).getRulesForIndexer();
        return exportService.exportToJson();
    }

    private static String loadFileToString(String fileName) {
        try {
            return IOUtils.readInputStream(DumperTest.class.getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
