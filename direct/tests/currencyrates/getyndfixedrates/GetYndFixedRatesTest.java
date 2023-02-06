package ru.yandex.autotests.directintapi.tests.currencyrates.getyndfixedrates;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.currencyratesservice.YndFixedRatesItem;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 30.09.15.
 * https://st.yandex-team.ru/TESTIRT-7338
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.CURRENCY_RATES)
@Issue("https://st.yandex-team.ru/DIRECT-46041")
@Description("Проверка ручки CurrencyRates.get_YND_FIXED_rates")
public class GetYndFixedRatesTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    private static Map<String, Map<String, YndFixedRatesItem>> response;

    @BeforeClass
    public static void makeRequest() {
        response = darkSideSteps.getCurrencyRates().getYndFixedRates();
    }

    @Test
    @Description("Ответ содержит данные о всех валютах")
    public void responseContainsAllCurrencies() {
        assertThat("ответ содержит данные о всех валютах", response.keySet(), containsInAnyOrder(
                Stream.of(Currency.values())
                        .filter(currency -> currency != Currency.YND_FIXED)
                        .map(Object::toString)
                        .toArray(String[]::new)));
    }

    @Test
    @Description("Каждое значение without_nds > 0")
    public void everyWithoutNdsGreaterThanZero() {
        boolean everyWithoutNdsGreaterThanZero = response.values().stream()
                .flatMap(map -> map.values().stream())
                .allMatch(item -> item.getWithoutNds() > 0);
        assertThat("значения without_nds > 0", everyWithoutNdsGreaterThanZero, equalTo(true));
    }

    @Test
    @Description("Каждое значение with_nds > 0")
    public void everyWithNdsGreaterThanZero() {
        boolean everyWithNdsGreaterThanZero = response.values().stream()
                .flatMap(map -> map.values().stream())
                .allMatch(item -> item.getWithNds() > 0);
        assertThat("значения with_nds > 0", everyWithNdsGreaterThanZero, equalTo(true));
    }

    @Test
    @Description("Значения without_nds <= withNds")
    public void withoutNdsLessThanOrEqualToWithNds() {
        boolean withoutNdsLessThanOrEqualToWithNds = response.values().stream()
                .flatMap(map -> map.values().stream())
                .allMatch(item -> item.getWithoutNds() <= item.getWithNds());
        assertThat("значения without_nds <= withNds", withoutNdsLessThanOrEqualToWithNds, equalTo(true));
    }

    @Test
    @Description("Для каждой валюты имеется курс \"с самого начала\" (значение \"0\")")
    public void everyCurrencyHasZeroKey() {
        boolean everyCurrencyHasZeroKey = response.values().stream()
                .allMatch(map -> map.containsKey("0"));
        assertThat("для каждой валюты имеется курс с ключём \"0\"", everyCurrencyHasZeroKey, equalTo(true));
    }
}
