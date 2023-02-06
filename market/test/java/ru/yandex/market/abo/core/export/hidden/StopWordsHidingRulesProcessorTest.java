package ru.yandex.market.abo.core.export.hidden;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.abo.core.hiding.rules.stopword.StopWordHidingRuleService;
import ru.yandex.market.abo.core.hiding.rules.stopword.model.OfferTag;
import ru.yandex.market.abo.core.hiding.rules.stopword.model.StopWordHidingRule;
import ru.yandex.market.abo.core.hiding.rules.stopword.model.StopWordRulesJson;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 15.03.18.
 */
public class StopWordsHidingRulesProcessorTest {
    private static final String JSON_PATH = "/hiding/export/stop_word_rules.json";

    @InjectMocks
    private StopWordsHidingRulesProcessor hidingRulesProcessor;
    @Mock
    private StopWordHidingRuleService hidingRuleService;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(hidingRuleService.exportForIndexer()).thenReturn(initRules());
    }

    @Test
    public void compareJson() throws IOException, JSONException {
        String json = IOUtils.toString(hidingRulesProcessor.getInputStream(), Charset.defaultCharset());
        String expectedJson = IOUtils.toString(
                StopWordsHidingRulesProcessorTest.class.getResourceAsStream(JSON_PATH), Charset.defaultCharset());

        JSONAssert.assertEquals(expectedJson, json, true);
    }

    private static StopWordRulesJson initRules() {
        return new StopWordRulesJson().setStopWordRules(Collections.singletonList(initStopWordRule()));
    }

    public static StopWordHidingRule initStopWordRule() {
        StopWordHidingRule rule = new StopWordHidingRule();
        rule.setStopWord("stopMePls");
        rule.setTags(Arrays.asList(OfferTag.sales_notes, OfferTag.description));
        rule.setCategoryWhitelistCsv("1, 2, 3");
        rule.setMorphology(true);
        rule.setForbiddenOnMarket(false);

        rule.setCreationTime(new Date());
        rule.setCreatedUserId(1L);
        rule.setDeleted(false);
        rule.setExcludeCutPrice(false);
        rule.setComment("internal comment");
        rule.setRgb(Sets.newHashSet(Color.RED, Color.BLUE));
        rule.setCreationTime(new Date(0));
        return rule;
    }

}
