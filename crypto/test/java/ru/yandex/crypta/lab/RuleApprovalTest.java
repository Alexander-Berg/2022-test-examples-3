package ru.yandex.crypta.lab;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.crypta.lab.proto.FullValue;
import ru.yandex.crypta.lab.proto.RuleCondition;
import ru.yandex.crypta.lab.utils.RuleApproval;

@RunWith(Parameterized.class)
public class RuleApprovalTest {
    private final String[] values;
    private final RuleCondition.Source source;
    private final RuleCondition.State state;

    private static Object[] approved(String[] values, RuleCondition.Source source) {
        return new Object[]{
                values,
                source,
                RuleCondition.State.APPROVED
        };
    }

    private static Object[] needsApproval(String[] values, RuleCondition.Source source) {
        return new Object[]{
                values,
                source,
                RuleCondition.State.NEED_APPROVE
        };
    }

    public RuleApprovalTest(String[] values, RuleCondition.Source source, RuleCondition.State state) {
        this.values = values;
        this.source = source;
        this.state = state;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> cases() {
        return Arrays.asList(needsApproval(
                        new String[]{"google.com", "k50.ru"},
                        RuleCondition.Source.YANDEX_REFERRER
                ),
                needsApproval(
                        new String[]{"auto.com"},
                        RuleCondition.Source.YANDEX_REFERRER
                ),
                needsApproval(
                        new String[]{"facebook.com/1", "facebook.com/2"},
                        RuleCondition.Source.YANDEX_REFERRER
                ),
                needsApproval(
                        new String[]{"facebook.com", "facebook.ru"},
                        RuleCondition.Source.YANDEX_REFERRER
                ),
                approved(
                        new String[]{"facebook.com", "google.ru"},
                        RuleCondition.Source.YANDEX_REFERRER
                ),
                needsApproval(
                        new String[]{"google.com"},
                        RuleCondition.Source.METRICA_SITES
                ),
                needsApproval(
                        new String[]{"zen.yandex.ru"},
                        RuleCondition.Source.YANDEX_REFERRER
                ),
                needsApproval(
                        new String[]{"ru.autoRu", "com.allgoritm.youla"},
                        RuleCondition.Source.APPS
                ),
                needsApproval(
                        new String[]{"com.allgoritm.youla"},
                        RuleCondition.Source.APPS
                ),
                approved(
                        new String[]{"com.allgoritm.youla", "com.sberbank"},
                        RuleCondition.Source.APPS
                ),
                needsApproval(
                        new String[]{"ru.yandex.taxi"},
                        RuleCondition.Source.APPS
                ),
                needsApproval(
                        new String[]{"something.yqtrack"},
                        RuleCondition.Source.APPS
                ),
                needsApproval(
                        new String[]{"regexp:acmp.ru/\\d+/123"},
                        RuleCondition.Source.PUBLIC_SITES
                ),
                approved(
                        new String[]{"google.com", "youtube.com"},
                        RuleCondition.Source.PUBLIC_SITES
                ),
                needsApproval(
                        new String[]{"google.com", "youtube.com", "regexp:acmp.ru/\\d+/123"},
                        RuleCondition.Source.PUBLIC_SITES
                ));
    }

    @Test
    public void test() {
        var fullValues =
                Arrays.stream(values).map(x -> FullValue.newBuilder().setNormalized(x).build()).collect(Collectors.toList());
        Assert.assertEquals(state, RuleApproval.getState(source, fullValues));
    }
}
