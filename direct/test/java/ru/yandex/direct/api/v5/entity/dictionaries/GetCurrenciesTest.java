package ru.yandex.direct.api.v5.entity.dictionaries;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.dictionaries.ConstantsItem;
import com.yandex.direct.api.v5.dictionaries.CurrenciesItem;
import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.dictionaries.exception.OldVersionOfAndroidApplicationException;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.CURRENCIES;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.common.db.PpcPropertyNames.GBP_CLIENT_UID;
import static ru.yandex.direct.common.db.PpcPropertyNames.USE_NEW_VALUES_FOR_RATE;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Api5Test
@RunWith(SpringRunner.class)
@Description("Получение валют")
public class GetCurrenciesTest {

    private static final int PROPERTIES_SIZE = 22;

    private static final String TRUE = "1";
    private static final String FALSE = "0";
    public static final String MAXIMUM_PAY_FOR_CONVERSION_CPA = "MaximumPayForConversionCPA";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiContextHolder apiContextHolder;

    @Autowired
    private Steps steps;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    private DictionariesService dictionariesService;

    private ClientInfo clientInfo;

    private DictionariesServiceBuilder builder;

    @Before
    public void before() {
        openMocks(this);

        builder = new DictionariesServiceBuilder(steps.applicationContext());

        clientInfo = steps.clientSteps().createDefaultClient();

        when(ppcPropertiesSupport.get(any(PpcPropertyName.class), any(Duration.class))).thenCallRealMethod();
        when(ppcPropertiesSupport.get(USE_NEW_VALUES_FOR_RATE.getName())).thenReturn(FALSE);
        when(ppcPropertiesSupport.get(GBP_CLIENT_UID.getName())).thenReturn("");
        builder.withPpcPropertiesSupport(ppcPropertiesSupport).withClientAuth(clientInfo);
    }

    @Test
    public void get_NoYndFixedCurrency() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<String> currencyNames = StreamEx.of(currencies).map(CurrenciesItem::getCurrency).toList();
        assertThat(currencyNames, everyItem(not("YND_FIXED")));
    }

    @Test
    public void get_NoRateProperty() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<String> propertyNames =
                StreamEx.of(currencies).flatCollection(CurrenciesItem::getProperties).map(ConstantsItem::getName).toList();

        assertThat(propertyNames, everyItem(not("Rate")));
    }

    @Test
    public void minimumBudgetProperty_ValueReturned_EqualToMinPayment() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        // Для рублей MinimumAccountDailyBudget не равен MinimumPayment, а задается непосредственно
        // константой MinimumAccountDailyBudget
        // https://st.yandex-team.ru/DIRECT-114397#5e562908f3c87714b0097565
        currencies =
                currencies.stream().filter(c -> c.getCurrency() != CurrencyCode.RUB.name()).collect(Collectors.toList());

        Map<String, String> accountDailyBudgetByCurrency = StreamEx.of(currencies)
                .mapToEntry(CurrenciesItem::getCurrency, constantsList -> StreamEx.of(constantsList.getProperties())
                        .filterBy(ConstantsItem::getName, "MinimumAccountDailyBudget")
                        .findFirst())
                .filterValues(Optional::isPresent)
                .mapValues(Optional::get)
                .mapValues(ConstantsItem::getValue)
                .toMap();

        Map<String, String> minPaymentByCurrency = StreamEx.of(currencies)
                .mapToEntry(CurrenciesItem::getCurrency, constantsList -> StreamEx.of(constantsList.getProperties())
                        .filterBy(ConstantsItem::getName, "MinimumPayment")
                        .findFirst())
                .filterValues(Optional::isPresent)
                .mapValues(Optional::get)
                .mapValues(ConstantsItem::getValue)
                .toMap();

        assertThat(accountDailyBudgetByCurrency.size(), is(currencies.size()));
        assertThat(accountDailyBudgetByCurrency, is(minPaymentByCurrency));
    }

    @Test
    public void get_NoRateWithVATProperty() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<String> propertyNames =
                StreamEx.of(currencies).flatCollection(CurrenciesItem::getProperties).map(ConstantsItem::getName).toList();

        assertThat(propertyNames, everyItem(not("RateWithVAT")));
    }

    @Test
    public void get_AndroidApp_RatePropertyNotReturned() {
        when(apiContextHolder.get().getPreAuthentication().isMobileApplication()).thenReturn(true);
        when(apiContextHolder.get().getPreAuthentication().isAndroidMobileApplication()).thenReturn(true);

        dictionariesService = builder
                .withApiContextHolder(apiContextHolder)
                .build();

        GetResponse response =
                dictionariesService.get(new GetRequest().withVersion("Ver").withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<List<String>> currenciesProperties = StreamEx.of(currencies)
                .map(CurrenciesItem::getProperties)
                .map(this::convertProperties)
                .toList();

        currenciesProperties.forEach(currencyProperties -> assertThat(currencyProperties, everyItem(not("Rate"))));
    }

    @Test
    public void get_AndroidApp_RateWithVATPropertyReturned() {
        when(apiContextHolder.get().getPreAuthentication().isMobileApplication()).thenReturn(true);
        when(apiContextHolder.get().getPreAuthentication().isAndroidMobileApplication()).thenReturn(true);

        dictionariesService = builder
                .withApiContextHolder(apiContextHolder)
                .build();

        GetResponse response =
                dictionariesService.get(new GetRequest().withVersion("Ver").withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<List<String>> currenciesProperties = StreamEx.of(currencies)
                .map(CurrenciesItem::getProperties)
                .map(this::convertProperties)
                .toList();

        currenciesProperties.forEach(currencyProperties -> assertThat(currencyProperties,
                everyItem(not("RateWithVAT"))));
    }

    @Test
    public void get_IosApp_NoRateProperty() {
        when(apiContextHolder.get().getPreAuthentication().isMobileApplication()).thenReturn(true);
        when(apiContextHolder.get().getPreAuthentication().isAndroidMobileApplication()).thenReturn(false);

        dictionariesService = builder
                .withApiContextHolder(apiContextHolder)
                .build();

        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<String> propertyNames =
                StreamEx.of(currencies).flatCollection(CurrenciesItem::getProperties).map(ConstantsItem::getName).toList();

        assertThat(propertyNames, everyItem(not("Rate")));
    }

    @Test
    public void get_IosApp_NoRateWithVATProperty() {
        when(apiContextHolder.get().getPreAuthentication().isMobileApplication()).thenReturn(true);
        when(apiContextHolder.get().getPreAuthentication().isAndroidMobileApplication()).thenReturn(false);

        dictionariesService = builder
                .withApiContextHolder(apiContextHolder)
                .build();

        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<String> propertyNames =
                StreamEx.of(currencies).flatCollection(CurrenciesItem::getProperties).map(ConstantsItem::getName).toList();

        assertThat(propertyNames, everyItem(not("RateWithVAT")));
    }

    @Test(expected = OldVersionOfAndroidApplicationException.class)
    public void get_ClientsMustUseNewVersionPropIsOn_OldAndroidApp_ExceptionThrown() {
        when(apiContextHolder.get().getPreAuthentication().isMobileApplication()).thenReturn(true);
        when(apiContextHolder.get().getPreAuthentication().isAndroidMobileApplication()).thenReturn(true);

        dictionariesService = builder
                .withApiContextHolder(apiContextHolder)
                .build();

        dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
    }

    @Test
    public void get_ClientsMustUseNewVersionPropIsOn_NewAndroidApp_NoExceptionThrown() {
        when(apiContextHolder.get().getPreAuthentication().isMobileApplication()).thenReturn(true);
        when(apiContextHolder.get().getPreAuthentication().isAndroidMobileApplication()).thenReturn(true);

        dictionariesService = builder
                .withApiContextHolder(apiContextHolder)
                .build();

        dictionariesService.get(new GetRequest().withVersion("Ver").withDictionaryNames(singletonList(CURRENCIES)));
    }

    @Test
    @Description("Количество значений в ответе равно количеству валют минус GBP минус YNDFixed")
    public void get_sizeTest() {
        dictionariesService = builder
                .withApiContextHolder(apiContextHolder)
                .build();

        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        assertEquals(Currencies.getCurrencies().size() - 2, currencies.size());
    }

    @Test
    @Description("Все валюты заполнены")
    public void get_currenciesAreFilledTest() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        List<String> values = currencies.stream().map(CurrenciesItem::getCurrency).collect(Collectors.toList());
        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Количество свойств верное")
    public void get_propertiesSizeTest() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        assertEquals(PROPERTIES_SIZE, currencies.get(0).getProperties().size());
    }

    @Test
    @Description("Все названия свойств заполнены")
    public void get_propertiesAreFilledTest() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        Set<String> values = currencies.stream()
                .flatMap(curr -> curr.getProperties().stream())
                .map(ConstantsItem::getName)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Все значения свойств заполнены")
    public void get_valuesAreFilledTest() {
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();

        Set<String> values = currencies.stream()
                .flatMap(curr -> curr.getProperties().stream())
                .map(ConstantsItem::getValue)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("При включение фичи отдаем максимальную цену конверсии увеличенную в 3 раза")
    public void get_TriplicationCpaLimitForPayForConversionEnabled_CpaPriceIncreased() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION, true);
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();
        Map<String, String> maxPayForConversionCpaByCurrency = getPropertyValueByCurrency(currencies,
                MAXIMUM_PAY_FOR_CONVERSION_CPA);

        Map<String, String> expected = Map.of(
                "CHF", "510000000",
                "EUR", "510000000",
                "KZT", "90000000000",
                "BYN", "480000000",
                "USD", "510000000",
                "TRY", "1350000000",
                "UAH", "3900000000",
                "RUB", "15000000000"
        );
        assertThat(maxPayForConversionCpaByCurrency, beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("Без включения фичи INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION отдаем максимальную неувеличенную " +
            "цену конверсии")
    public void get_CpaLimitForPayForConversionEnabled_CpaPriceNormal() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION, false);
        dictionariesService = builder.build();
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CURRENCIES)));
        List<CurrenciesItem> currencies = response.getCurrencies();
        Map<String, String> maxPayForConversionCpaByCurrency = getPropertyValueByCurrency(currencies,
                MAXIMUM_PAY_FOR_CONVERSION_CPA);

        Map<String, String> expected = Map.of(
                "CHF", "170000000",
                "EUR", "170000000",
                "KZT", "30000000000",
                "BYN", "160000000",
                "USD", "170000000",
                "TRY", "450000000",
                "UAH", "1300000000",
                "RUB", "5000000000"
        );
        assertThat(maxPayForConversionCpaByCurrency, beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }


    private static Map<String, String> getPropertyValueByCurrency(List<CurrenciesItem> currencies,
                                                                  String propertyName) {
        return StreamEx.of(currencies)
                .mapToEntry(CurrenciesItem::getCurrency, x -> x.getProperties().stream()
                        .filter(z -> z.getName().equals(propertyName))
                        .findAny()
                        .get()
                        .getValue())
                .toMap();
    }

    private List<String> convertProperties(List<ConstantsItem> constants) {
        return mapList(constants, ConstantsItem::getName);
    }
}
