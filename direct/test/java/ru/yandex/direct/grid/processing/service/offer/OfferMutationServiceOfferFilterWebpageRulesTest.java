package ru.yandex.direct.grid.processing.service.offer;

import java.util.List;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants;
import ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOffer;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.util.OfferTestDataUtils;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTarget;

@GridProcessingTest
@RunWith(Parameterized.class)
public class OfferMutationServiceOfferFilterWebpageRulesTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private OfferMutationService offerMutationService;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<WebpageRule> initialRules;

    @Parameterized.Parameter(2)
    public List<String> urls;

    @Parameterized.Parameter(3)
    public List<WebpageRule> expectedRules;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                {"пустые списки",
                        List.of(),
                        List.of(),
                        List.of()},
                {"один товар - нет отфильтрованных товаров (нет правил)",
                        List.of(),
                        List.of(url(0)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(0)))},
                {"один товар - нет отфильтрованных товаров (null-type правило)",
                        List.of(rule(null, null, null)),
                        List.of(url(0)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                urls(0)))},
                {"один товар - нет отфильтрованных товаров (ANY-type правило)",
                        List.of(rule(WebpageRuleType.ANY, null, null)),
                        List.of(url(0)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                urls(0)))},
                {"один товар - есть другие отфильтрованные товары",
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(1))),
                        List.of(url(0)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(1, 0)))},
                {"один товар - есть отфильтрованные по другим признакам товары",
                        List.of(rule(WebpageRuleType.DOMAIN, WebpageRuleKind.EXACT, List.of("example.com"))),
                        List.of(url(0)),
                        List.of(rule(WebpageRuleType.DOMAIN, WebpageRuleKind.EXACT, List.of("example.com")),
                                rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(0)))},
                {"один товар - товар уже отфильтрован",
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(0, 1))),
                        List.of(url(0)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(0, 1)))},
                {"один товар - есть полностью заполненное условие",
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                urls(IntStreamEx.range(DynamicTextAdTargetConstants.MAX_ARGUMENTS)))),
                        List.of(url(DynamicTextAdTargetConstants.MAX_ARGUMENTS)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                urls(IntStreamEx.range(DynamicTextAdTargetConstants.MAX_ARGUMENTS))),
                                rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(DynamicTextAdTargetConstants.MAX_ARGUMENTS)))},
                {"несколько товаров - нет отфильтрованных товаров, не влезет в одно условие",
                        List.of(),
                        IntStreamEx.range(DynamicTextAdTargetConstants.MAX_ARGUMENTS * 2)
                                .mapToObj(OfferMutationServiceOfferFilterWebpageRulesTest::url)
                                .toList(),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                urls(IntStreamEx.range(DynamicTextAdTargetConstants.MAX_ARGUMENTS))),
                                rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(IntStreamEx.range(DynamicTextAdTargetConstants.MAX_ARGUMENTS,
                                                DynamicTextAdTargetConstants.MAX_ARGUMENTS * 2))))},
                {"несколько товаров - часть уже отфильтрована",
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(1, 3))),
                        List.of(url(0), url(2)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(1, 3, 0, 2)))},
                {"несколько товаров - переполнит существующее условие",
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(IntStreamEx.range(DynamicTextAdTargetConstants.MAX_ARGUMENTS - 1)))),
                        List.of(url(DynamicTextAdTargetConstants.MAX_ARGUMENTS - 1),
                                url(DynamicTextAdTargetConstants.MAX_ARGUMENTS)),
                        List.of(rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(IntStreamEx.range(DynamicTextAdTargetConstants.MAX_ARGUMENTS))),
                                rule(WebpageRuleType.URL_PRODLIST, WebpageRuleKind.NOT_EQUALS,
                                        urls(DynamicTextAdTargetConstants.MAX_ARGUMENTS)))}
        };
    }

    private static WebpageRule rule(WebpageRuleType type, WebpageRuleKind kind, List<String> value) {
        return new WebpageRule()
                .withType(type)
                .withKind(kind)
                .withValue(value);
    }

    private static List<String> urls(int... ids) {
        return urls(IntStreamEx.of(ids));
    }

    private static List<String> urls(IntStreamEx ids) {
        return ids.mapToObj(OfferMutationServiceOfferFilterWebpageRulesTest::url).toList();
    }

    private static String url(int id) {
        return String.format("https://example.com/%d", id);
    }

    @Test
    public void testDynamicTextAdTarget() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        DynamicTextAdTargetInfo dynamicTextAdTargetInfo = steps.dynamicTextAdTargetsSteps()
                .createDynamicTextAdTarget(adGroupInfo, defaultDynamicTextAdTarget(adGroupInfo)
                        .withCondition(initialRules));
        DynamicTextAdTarget dynamicTextAdTarget = dynamicTextAdTargetInfo.getDynamicTextAdTarget();

        List<GdiOffer> offers = StreamEx.generate(OfferTestDataUtils::defaultGdiOffer)
                .zipWith(urls.stream()).mapKeyValue(GdiOffer::withUrl).toList();

        ModelChanges<DynamicTextAdTarget> modelChanges = offerMutationService
                .getOfferFilterModelChanges(dynamicTextAdTarget, offers);

        assertThat(modelChanges.getChangedPropsNames())
                .containsOnly(DynamicTextAdTarget.CONDITION, DynamicTextAdTarget.CONDITION_HASH,
                        DynamicTextAdTarget.CONDITION_UNIQ_HASH);
        assertThat(modelChanges.getChangedProp(DynamicTextAdTarget.CONDITION))
                .containsExactlyElementsOf(expectedRules);
        assertThat(modelChanges.getChangedProp(DynamicTextAdTarget.CONDITION_HASH))
                .isEqualTo(DynamicTextAdTargetHashUtils.getHash(expectedRules));
        assertThat(modelChanges.getChangedProp(DynamicTextAdTarget.CONDITION_UNIQ_HASH))
                .isEqualTo(DynamicTextAdTargetHashUtils.getUniqHash(expectedRules));
    }
}
