package ru.yandex.market.api.internal.report.parsers.json.filters;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.domain.v2.FilterField;
import ru.yandex.market.api.domain.v2.filters.ColorEnumValue;
import ru.yandex.market.api.domain.v2.filters.EnumFilter;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.domain.v2.filters.FilterRedirect;
import ru.yandex.market.api.domain.v2.filters.FilterValue;
import ru.yandex.market.api.domain.v2.filters.NumericFilter;
import ru.yandex.market.api.domain.v2.filters.SizeEnumValue;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.matchers.FilterValueMatcher;
import ru.yandex.market.api.matchers.PhotoPickerEnumValueMatchers;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.api.util.parser.Enums;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.PhotoPickerEnumValueMatchers.photoPickerValue;

/**
 * Created by apershukov on 09.11.16.
 */
@WithContext
@WithMocks
public class FilterV2JsonParserTest extends BaseTest {

    @Mock
    ClientHelper clientHelper;

    private FilterV2JsonParser parser;

    MockClientHelper mockClientHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.setUrlSchema(UrlSchema.HTTP);

        parser = new FilterV2JsonParser(Enums.allOf(FilterField.class), new FilterFactory());
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void testParseNumberFilter() {
        NumericFilter filter = (NumericFilter) parse("number-filter.json");

        assertEquals("NUMBER", filter.getType());
        assertEquals("Длина спального места", filter.getName());
        assertEquals("12442823", filter.getId());

        assertEquals("160", filter.getMin());
        assertEquals("220", filter.getMax());
        assertEquals(2, filter.getPrecision());
    }

    @Test
    public void testParseRangeFilter() {
        NumericFilter filter = (NumericFilter) parse("range-filter.json");

        assertEquals("Ширина", filter.getName());
        assertEquals("NUMBER", filter.getType());
        assertEquals("6187317", filter.getId());

        assertEquals("50", filter.getMin());
        assertEquals("241", filter.getMax());
    }

    @Test
    public void testParsePromoTypeFilter() {
        EnumFilter filter = (EnumFilter) parse("promo-type-filter.json");

        assertEquals("Тип акции", filter.getName());
        assertEquals("-18", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();

        assertEquals("n-plus-m", values.get(0).getId());
        assertEquals("Подарок при покупке определенного количества", values.get(0).getName());

        assertEquals("gift-with-purchase", values.get(1).getId());
        assertEquals("Подарок при покупке", values.get(1).getName());

        assertEquals("service-with-purchase", values.get(2).getId());
        assertEquals("Обслуживание при покупке", values.get(2).getName());

        assertEquals("second-offer-discount", values.get(3).getId());
        assertEquals("Скидка на следующую покупку", values.get(3).getName());
    }

    @Test
    public void testParseDeliveryTypeFilter() {
        EnumFilter filter = (EnumFilter) parse("offer-shipping-filter.json");

        assertEquals("Способ доставки", filter.getName());
        assertEquals("-10", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();

        assertEquals(3, values.size());

        assertEquals("delivery", values.get(0).getId());
        assertEquals("С доставкой", values.get(0).getName());

        assertEquals("pickup", values.get(1).getId());
        assertEquals("Самовывоз", values.get(1).getName());

        assertEquals("postomat", values.get(2).getId());
        assertEquals("Почтоматы Маркета", values.get(2).getName());
    }

    @Test
    public void testParseDeliveryIntervalFilter() {
        EnumFilter filter = (EnumFilter) parse("offer-delivery-interval-filter.json");

        assertEquals("Срок доставки курьером", filter.getName());
        assertEquals("-14", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();

        assertEquals(3, values.size());

        assertEquals("0", values.get(0).getId());
        assertEquals("Сегодня", values.get(0).getName());
        assertEquals(Long.valueOf(0), values.get(0).getFound());

        assertEquals("1", values.get(1).getId());
        assertEquals("Завтра", values.get(1).getName());
        assertEquals(Long.valueOf(0), values.get(1).getFound());

        assertEquals("5", values.get(2).getId());
        assertEquals("До 5 дней", values.get(2).getName());
        assertEquals(Long.valueOf(3), values.get(2).getFound());
    }

    @Test
    public void testParseDeliveryIntervalFilterThatLooksLikeBoolean() {
        EnumFilter filter = (EnumFilter) parse("offer-delivery-interval-filter-like-boolean.json");

        assertEquals("Срок доставки курьером", filter.getName());
        assertEquals("-14", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();

        assertEquals(2, values.size());

        assertEquals("0", values.get(0).getId());
        assertEquals("Сегодня", values.get(0).getName());
        assertEquals(Long.valueOf(2), values.get(0).getFound());

        assertEquals("1", values.get(1).getId());
        assertEquals("Завтра", values.get(1).getName());
        assertEquals(Long.valueOf(1), values.get(1).getFound());
    }

    /**
     * Тестирование того что при парсинге фильтра "Способ доситавки" для мобильного
     * приложения пропускаются пункт "Почтоматы Маркета"
     */
    @Test
    public void testParseDeliveryTypeFilterForMobile() {
        ContextHolder.get().setClient(new Client() {{
            setType(Type.MOBILE);
        }});

        EnumFilter filter = (EnumFilter) parse("offer-shipping-filter.json");

        assertEquals("Способ доставки", filter.getName());
        assertEquals("-10", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();

        assertEquals(2, values.size());

        assertEquals("delivery", values.get(0).getId());
        assertEquals("С доставкой", values.get(0).getName());

        assertEquals("pickup", values.get(1).getId());
        assertEquals("Самовывоз", values.get(1).getName());
    }

    @Test
    public void testParseDeliveryTypeFilterWithUnknownDelivery() {
        EnumFilter filter = (EnumFilter) parse("offer-shipping-filter-unknown.json");

        assertEquals("Способ доставки", filter.getName());
        assertEquals("-10", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();

        assertEquals(3, values.size());

        assertEquals("delivery", values.get(0).getId());
        assertEquals("С доставкой", values.get(0).getName());

        assertEquals("pickup", values.get(1).getId());
        assertEquals("Самовывоз", values.get(1).getName());

        assertEquals("postomat", values.get(2).getId());
        assertEquals("Почтоматы Маркета", values.get(2).getName());
    }

    @Test
    public void shouldParsePriceFilter() {
        NumericFilter filter = (NumericFilter) parse("price-filter.json");

        assertEquals("-1", filter.getId());
        assertEquals("Цена", filter.getName());
        assertEquals("15498648", filter.getMax());
        assertEquals("16167.9078", filter.getMin());
        assertEquals("10000~20000000", filter.getValue());
    }

    /**
     * Тестирование того что enum-фильтр с одним значением не парсится
     */
    @Test
    public void testSkipEnumFilterWithSingleValue() {
        Filter filter = parse("single-color-filter.json");
        assertNull(filter);
    }

    /**
     * Тестирование того что enum-фильтр с одним выбранным значением парсится
     */
    @Test
    public void testParseEnumFilterWithSingleCheckedValue() {
        Filter filter = parse("single-color-filter-checked.json");
        assertNotNull(filter);
    }

    @Test
    public void testParseHasBoolNoFilterAsRadio() {
        EnumFilter filter = (EnumFilter) parse("has-bool-no-filter.json");

        assertEquals("4925668", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();
        assertEquals(3, values.size());

        assertEquals("0", values.get(0).getId());
        assertEquals("Нет", values.get(0).getName());
        assertEquals(111, (long) values.get(0).getInitialFound());
        assertEquals(222, (long) values.get(0).getFound());

        assertEquals("1", values.get(1).getId());
        assertEquals("Да", values.get(1).getName());
        assertEquals(112, (long) values.get(1).getInitialFound());
        assertEquals(223, (long) values.get(1).getFound());

        assertEquals("any", values.get(2).getId());
        assertEquals("Не важно", values.get(2).getName());
        assertEquals(223, (long) values.get(2).getInitialFound());
        assertEquals(445, (long) values.get(2).getFound());
    }

    @Test
    public void testParseShopRatingFilter() {
        EnumFilter filter = (EnumFilter) parse("shop-rating-filter.json");

        assertEquals("Рейтинг магазина", filter.getName());
        assertEquals("-5", filter.getId());
        assertEquals("RADIO", filter.getType());

        List<FilterValue> values = filter.getValues();

        assertEquals(3, values.size());

        assertEquals("2", values.get(0).getId());
        assertEquals("От 2-x звезд и выше", values.get(0).getName());

        assertEquals("3", values.get(1).getId());
        assertEquals("От 3-x звезд и выше", values.get(1).getName());

        assertEquals("4", values.get(2).getId());
        assertEquals("От 4-x звезд и выше", values.get(2).getName());
    }

    @Test
    public void testParseColorFilter() {
        EnumFilter filter = (EnumFilter) parse("color-filter.json");

        assertEquals("13354415", filter.getId());
        assertEquals("COLOR", filter.getType());
        assertEquals("Цвет", filter.getName());

        List<ColorEnumValue> values = (List<ColorEnumValue>) (List) filter.getValues();
        assertEquals(3, values.size());

        assertEquals("13354443", values.get(0).getId());
        assertEquals("белый", values.get(0).getName());
        assertEquals("#FFFFFF", values.get(0).getColor());

        assertEquals("13354528", values.get(1).getId());
        assertEquals("золотистый", values.get(1).getName());
        assertEquals("#FFD700", values.get(1).getColor());

        assertEquals("13354600", values.get(2).getId());
        assertEquals("розовый", values.get(2).getName());
        assertEquals("#FF00BB", values.get(2).getColor());
    }

    @Test
    public void testParseEnumFilter() {
        EnumFilter filter = (EnumFilter) parse("enum-filter.json");

        assertEquals("4940921", filter.getId());
        assertEquals("ENUM", filter.getType());
        assertEquals("Тип", filter.getName());

        List<FilterValue> values = filter.getValues();
        assertEquals(4, values.size());

        assertEquals("13475069", values.get(0).getId());
        assertEquals("смартфон", values.get(0).getName());
        assertEquals(586978, (long) values.get(0).getInitialFound());
        assertEquals(586978, (long) values.get(0).getFound());

        assertEquals("12105575", values.get(1).getId());
        assertEquals("телефон", values.get(1).getName());
        assertEquals(122621, (long) values.get(1).getInitialFound());
        assertEquals(122621, (long) values.get(1).getFound());

        assertEquals("13475319", values.get(2).getId());
        assertEquals("телефон для детей", values.get(2).getName());
        assertEquals(415, (long) values.get(2).getInitialFound());
        assertEquals(415, (long) values.get(2).getFound());

        assertEquals("13475071", values.get(3).getId());
        assertEquals("телефон для пожилых", values.get(3).getName());
        assertEquals(3798, (long) values.get(3).getInitialFound());
        assertEquals(3700, (long) values.get(3).getFound());
    }

    @Test
    public void testParseEnumFilterWithRedirect() {
        EnumFilter filter = (EnumFilter) parse("enum-filters-with-redirect.json");

        assertEquals("11033928", filter.getId());
        assertEquals("ENUM", filter.getType());
        assertEquals("Тип", filter.getName());
        assertEquals("Чехлы для мобильных телефонов", filter.getCategoryName());
        assertEquals(true, filter.getRedirect());

        List<FilterValue> values = filter.getValues();
        assertEquals(9, values.size());

        assertEquals("11033931", values.get(0).getId());
        assertEquals("бампер", values.get(0).getName());
        assertEquals(26, (long) values.get(0).getInitialFound());
        assertEquals(26, (long) values.get(0).getFound());
        assertEquals(91498, (long) values.get(0).getHid());
        assertEquals(56036, (long) values.get(0).getNid());

        assertEquals("16403467", values.get(1).getId());
        assertEquals("гермочехол", values.get(1).getName());
        assertEquals(5, (long) values.get(1).getInitialFound());
        assertEquals(5, (long) values.get(1).getFound());
        assertEquals(91498, (long) values.get(1).getHid());
        assertEquals(56036, (long) values.get(1).getNid());

        assertEquals("11033941", values.get(2).getId());
        assertEquals("книжка", values.get(2).getName());
        assertEquals(507, (long) values.get(2).getInitialFound());
        assertEquals(507, (long) values.get(2).getFound());
        assertEquals(91498, (long) values.get(2).getHid());
        assertEquals(56036, (long) values.get(2).getNid());

        assertEquals("11043202", values.get(3).getId());
        assertEquals("накладка", values.get(3).getName());
        assertEquals(2750, (long) values.get(3).getInitialFound());
        assertEquals(2750, (long) values.get(3).getFound());
        assertEquals(91498, (long) values.get(3).getHid());
        assertEquals(56036, (long) values.get(3).getNid());

        // все 9 штук проверять не буду
        assertEquals("16571103", values.get(8).getId());
        assertEquals("футляр", values.get(8).getName());
        assertEquals(1, (long) values.get(8).getInitialFound());
        assertEquals(1, (long) values.get(8).getFound());
        assertEquals(91498, (long) values.get(8).getHid());
        assertEquals(56036, (long) values.get(8).getNid());
    }

    @Test
    public void testParseSizeFilter() {
        EnumFilter filter = (EnumFilter) parse("size-filter.json");

        assertEquals("8224549", filter.getId());
        assertEquals("SIZE", filter.getType());
        assertEquals("Рост", filter.getName());
        assertEquals("см", filter.getUnit());
        assertEquals("дюймы", filter.getDefaultUnit());

        List<SizeEnumValue> values = (List<SizeEnumValue>) (List) filter.getValues();
        assertEquals(4, values.size());

        assertEquals("18446744073709549374", values.get(0).getId());
        assertEquals("28", values.get(0).getName());
        assertEquals("дюймы", values.get(0).getUnitId());

        assertEquals("18446744073709549373", values.get(1).getId());
        assertEquals("30", values.get(1).getName());
        assertEquals("дюймы", values.get(1).getUnitId());

        assertEquals("18446744073709549626", values.get(2).getId());
        assertEquals("38", values.get(2).getName());
        assertEquals("RU", values.get(2).getUnitId());

        assertEquals("18446744073709549625", values.get(3).getId());
        assertEquals("40", values.get(3).getName());
        assertEquals("RU", values.get(3).getUnitId());
    }

    @Test
    public void testParseSizeFilterFormMobile() {
        ContextHolder.get().setClient(new Client() {{
            setType(Type.MOBILE);
        }});

        EnumFilter filter = (EnumFilter) parse("size-filter.json");

        assertEquals("8224549", filter.getId());
        assertEquals("SIZE", filter.getType());
        assertEquals("Рост", filter.getName());
        assertEquals("см", filter.getUnit());
        assertEquals("дюймы", filter.getDefaultUnit());

        List<SizeEnumValue> values = (List<SizeEnumValue>) (List) filter.getValues();
        assertEquals(2, values.size());

        assertEquals("18446744073709549374", values.get(0).getId());
        assertEquals("28", values.get(0).getName());
        assertEquals("дюймы", values.get(0).getUnitId());

        assertEquals("18446744073709549373", values.get(1).getId());
        assertEquals("30", values.get(1).getName());
        assertEquals("дюймы", values.get(1).getUnitId());
    }

    @Test
    public void testParseSimpleBooleanFilter() {
        EnumFilter filter = (EnumFilter) parse("boolean-filter.json");

        assertEquals("7924631", filter.getId());
        assertEquals("BOOLEAN", filter.getType());
        assertEquals("Рваные", filter.getName());

        List<FilterValue> values = filter.getValues();
        assertEquals(2, values.size());

        assertEquals("0", values.get(0).getId());
        assertEquals("0", values.get(0).getName());

        assertEquals("1", values.get(1).getId());
        assertEquals("1", values.get(1).getName());
    }

    @Test
    public void testSkipFilterOfUnknownType() {
        EnumFilter filter = (EnumFilter) parse("unknown-type-filter.json");
        assertNull(filter);
    }

    @Test
    public void testColorFilterWithPhotoPicker() {
        EnumFilter filter = (EnumFilter) parse("color-filter-with-image-picker.json");

        assertThat(filter.getId(), is("14871214"));
        assertThat(filter.getType(), is("PHOTO_PICKER"));
        assertThat(filter.getName(), is("Цвет товара"));

        assertThat(
                cast(filter.getValues()),
                containsInAnyOrder(
                        photoPickerValue(
                                PhotoPickerEnumValueMatchers.id("15266392"),
                                PhotoPickerEnumValueMatchers.color(null),
                                PhotoPickerEnumValueMatchers.photo("http://avatars.mds.yandex" +
                                        ".net/get-mpic/466729/model_option-picker-1732171388-15266392" +
                                        "--84c6daccab203dc43a44a5db6c56566a/1")
                        ),
                        photoPickerValue(
                                PhotoPickerEnumValueMatchers.id("14897638"),
                                PhotoPickerEnumValueMatchers.color("#C0C0C0"),
                                PhotoPickerEnumValueMatchers.photo("http://avatars.mds.yandex" +
                                        ".net/get-mpic/397397/model_option-picker-1732171388-14897638" +
                                        "--32188357156685ff3f046d6533e32d7c/1")
                        ),
                        photoPickerValue(
                                PhotoPickerEnumValueMatchers.id("15277521"),
                                PhotoPickerEnumValueMatchers.color("#808080"),
                                PhotoPickerEnumValueMatchers.photo("http://avatars.mds.yandex" +
                                        ".net/get-mpic/397397/model_option-picker-1732171388-15277521" +
                                        "--5b15e2df58874c91c61b6391df2aaf40/1"),
                                PhotoPickerEnumValueMatchers.checked(true)
                        )
                )
        );

    }

    @Test
    public void testFuzzyFitler() {
        EnumFilter filter = (EnumFilter) parse("fuzzy-filter.json");

        assertThat(filter.getId(), is("13887626"));
        assertThat(filter.getType(), is("COLOR"));
        assertThat(filter.getName(), is("Цвет"));


        assertThat(
                cast(filter.getValues()),
                containsInAnyOrder(
                        FilterValueMatcher.fuzzy(false),
                        FilterValueMatcher.fuzzy(true),
                        FilterValueMatcher.fuzzy(true)
                )

        );
    }

    @Test
    public void testSkipFilterWithEmptyValues() {
        Filter filter = parse("empty-values-filter.json");
        Assert.assertNull(filter);
    }

    @Test
    public void testUnsignedLong() {
        EnumFilter filter = (EnumFilter) parse("unsigned-long.json");
        assertEquals(new Long(Integer.MAX_VALUE), filter.getValues().get(0).getInitialFound());
    }

    @Test
    public void checkedInSizedFilter() {
        EnumFilter filter = (EnumFilter) parse("size-checked.json");
        assertThat(
                cast(filter.getValues()),
                containsInAnyOrder(
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557966"),
                                FilterValueMatcher.checked(null)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557748"),
                                FilterValueMatcher.checked(null)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557760"),
                                FilterValueMatcher.checked(null)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557773"),
                                FilterValueMatcher.checked(null)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557786"),
                                FilterValueMatcher.checked(true)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557799"),
                                FilterValueMatcher.checked(true)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557812"),
                                FilterValueMatcher.checked(null)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557825"),
                                FilterValueMatcher.checked(null)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557838"),
                                FilterValueMatcher.checked(null)
                        ),
                        FilterValueMatcher.filterValue(
                                FilterValueMatcher.id("14557852"),
                                FilterValueMatcher.checked(null)
                        )
                )

        );
    }

    @Test
    public void shouldParseFilterRedirect() {
        Filter filter = parse("filter_with_redirect.json");
        FilterRedirect filterRedirect = filter.getFilterRedirect();
        assertNotNull(filterRedirect);
        assertThat(filterRedirect.getHid().getId(), is(10682526L));
        assertThat(filterRedirect.getHid().getSlug(), is("razvivaiushchie-igrushki"));
        assertThat(filterRedirect.getNid().getId(), is(59709L));
        assertThat(filterRedirect.getNid().getSlug(), is("razvivaiushchie-igrushki-dlia-malyshei"));
    }

    private Filter parse(String path) {
        return parser.parse(ResourceHelpers.getResource(path));
    }

    private static <T extends FilterValue> List<T> cast(List<FilterValue> filters) {
        return (List<T>) filters;
    }
}
