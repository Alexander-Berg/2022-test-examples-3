package ru.yandex.direct.core.entity.dynamictextadtarget.utils;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHash;

@RunWith(Parameterized.class)
public class DynamicTextAdTargetConditionHashCompareDBTest {

    // значения хешей взяты из базы. а в базу добавлены через перл реализацию
    @Parameterized.Parameters(name = "condition: {1}, hash: {2}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{singletonList(new WebpageRule().withType(WebpageRuleType.ANY)), "[{\"type\":\"any\"}]",
                        "9243564304133471762"},
                new Object[]{emptyList(), "[]", "15515306683186839187"},

                new Object[]{singletonList(
                        new WebpageRule().withType(WebpageRuleType.URL).withKind(WebpageRuleKind.EXACT)
                                .withValue(singletonList("mobile/mobilnye-telefony-i-smartfony"))),
                        "[{\"kind\":\"exact\",\"type\":\"URL\",\"value\":[\"mobile/mobilnye-telefony-i-smartfony\"]}]",
                        "8425425181215663668"}
        );
    }

    private List<WebpageRule> rules;
    private String rulesJson;
    private String rulesHash;

    public DynamicTextAdTargetConditionHashCompareDBTest(
            List<WebpageRule> rules, String rulesJson, String rulesHash) {
        this.rules = rules;
        this.rulesJson = rulesJson;
        this.rulesHash = rulesHash;
    }

    @Test
    public void test() {
        assertEquals(getHash(rulesJson), new BigInteger(rulesHash));
        assertEquals(getHash(rules), new BigInteger(rulesHash));
    }
}
