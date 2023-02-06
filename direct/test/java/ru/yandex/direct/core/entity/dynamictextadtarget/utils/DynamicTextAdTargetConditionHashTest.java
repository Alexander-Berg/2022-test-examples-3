package ru.yandex.direct.core.entity.dynamictextadtarget.utils;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHash;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getUniqHash;

public class DynamicTextAdTargetConditionHashTest {

    @Test
    public void testHashAndUniqHash() {
        List<WebpageRule> rules1 = asList(new WebpageRule()
                        .withType(WebpageRuleType.URL)
                        .withKind(WebpageRuleKind.EXACT)
                        .withValue(asList("a")),
                new WebpageRule()
                        .withType(WebpageRuleType.URL)
                        .withKind(WebpageRuleKind.EXACT)
                        .withValue(asList("b")));

        List<WebpageRule> rules2 = asList(new WebpageRule()
                        .withType(WebpageRuleType.URL)
                        .withKind(WebpageRuleKind.EXACT)
                        .withValue(asList("b")),
                new WebpageRule()
                        .withType(WebpageRuleType.URL)
                        .withKind(WebpageRuleKind.EXACT)
                        .withValue(asList("a")));

        assertEquals(getUniqHash(rules1), getUniqHash(rules2));
        assertNotEquals(getHash(rules1), getHash(rules2));
    }
}
