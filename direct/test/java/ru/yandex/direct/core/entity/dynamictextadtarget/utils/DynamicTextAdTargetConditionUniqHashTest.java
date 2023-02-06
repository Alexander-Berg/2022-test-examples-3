package ru.yandex.direct.core.entity.dynamictextadtarget.utils;

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
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getUniqHash;

@RunWith(Parameterized.class)
public class DynamicTextAdTargetConditionUniqHashTest {


    @Parameterized.Parameters(name = "rules1: {0}, rules2: {1}, isEqual: {2}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{
                        singletonList(new WebpageRule().withType(WebpageRuleType.ANY)),
                        singletonList(new WebpageRule().withType(WebpageRuleType.ANY)),
                        true},

                new Object[]{emptyList(),
                        emptyList(), true},

                // single rule, same type

                new Object[]{
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        true},

                new Object[]{
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("b"))),
                        false},

                new Object[]{
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a", "a"))),
                        true},

                new Object[]{
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a", "aaaa"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("aa", "aaa"))),
                        false},

                new Object[]{
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a", "b"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("b", "a"))),
                        true},

                // single rule, different type
                new Object[]{
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.NOT_EQUALS)
                                .withValue(asList("a"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        false},

                new Object[]{
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.CONTENT)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.TITLE)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        false},

                // multiple rules
                new Object[]{
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("a"))),
                        true},

                new Object[]{
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a", "b")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("b", "a"))),
                        singletonList(new WebpageRule()
                                .withType(WebpageRuleType.URL)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(asList("b", "a", "a"))),
                        true},

                new Object[]{
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a", "b")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("b", "a"))),
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a", "b")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("b", "a"))),
                        true},

                new Object[]{
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a", "b")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.TITLE)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("b", "a"))),
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.TITLE)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("b", "a")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a", "b"))),
                        true},

                new Object[]{
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a", "b")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.TITLE)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("b", "a"))),
                        asList(new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("b", "a")),
                                new WebpageRule()
                                        .withType(WebpageRuleType.URL)
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withValue(asList("a", "b"))),
                        false}

        );
    }

    private List<WebpageRule> rules1;
    private List<WebpageRule> rules2;
    private boolean isEqual;

    public DynamicTextAdTargetConditionUniqHashTest(
            List<WebpageRule> rules1,
            List<WebpageRule> rules2, boolean isEqual) {
        this.rules1 = rules1;
        this.rules2 = rules2;
        this.isEqual = isEqual;
    }

    @Test
    public void test() {
        assertEquals(getUniqHash(rules1) == getUniqHash(rules2), isEqual);
    }
}
