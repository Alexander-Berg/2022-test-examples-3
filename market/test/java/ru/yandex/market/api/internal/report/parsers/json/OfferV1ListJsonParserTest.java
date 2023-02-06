package ru.yandex.market.api.internal.report.parsers.json;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.api.category.BoolFilter;
import ru.yandex.market.api.category.EnumFilter;
import ru.yandex.market.api.category.EnumFilterType;
import ru.yandex.market.api.category.EnumValue;
import ru.yandex.market.api.category.EnumValueShop;
import ru.yandex.market.api.category.Filter;
import ru.yandex.market.api.category.FilterSubType;
import ru.yandex.market.api.category.FilterType;
import ru.yandex.market.api.category.NumericFilter;
import ru.yandex.market.api.category.SimpleFilter;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.controller.v2.TextParameters;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.offer.OfferFieldV1;
import ru.yandex.market.api.offer.Offers;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.common.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by anton0xf on 07.03.17.
 */
@WithContext
public class OfferV1ListJsonParserTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(OfferV1ListJsonParserTest.class);

    @Inject
    private ReportParserFactory reportParserFactory;

    private Parser<Offers> getParser(Collection<Field> fields) {
        ReportRequestContext reportContext = new ReportRequestContext();

        reportContext.setFields(fields);
        reportContext.setUserRegionId(GeoUtils.DEFAULT_GEO_ID);
        return reportParserFactory.getOfferV1ListJsonParser(reportContext, new PageInfo());
    }

    @Test
    public void parse() {
        Offers offers = getParser(Collections.emptyList())
            .parse(ResourceHelpers.getResource("productoffers.json"));
        assertNotNull(offers.getItems());
        assertEquals(2, offers.getCount());
        assertEquals(2, offers.getItems().size());
        assertEquals(Integer.valueOf(1), offers.getRegionDelimiterPosition());
        assertNull(offers.getFilters());
    }

    @Test
    public void parseFilters() {
        Offers offers = getParser(Collections.singleton(OfferFieldV1.FILTERS))
            .parse(ResourceHelpers.getResource("productoffers.json"));
        List<Filter> filters = offers.getFilters();
        LOG.info("filters: {}", filters);
        assertNotNull(filters);
        assertEquals(17, filters.size());
        Map<String, SimpleFilter> filtersMap = filters.stream()
            .collect(Collectors.toMap(Filter::getId, v -> (SimpleFilter) v));
        checkPrice(filtersMap);
        checkCpu(filtersMap);
        checkWiFi(filtersMap);
        checkDelivery(filtersMap);
        checkShops(filtersMap);
        checkText(filtersMap);
    }

    private void checkPrice(Map<String, SimpleFilter> filters) {
        SimpleFilter filter = filters.get(Filters.PRICE_FILTER_CODE);
        assertNotNull(filter);
        assertEquals("Цена", filter.getName());
        assertEquals("price", filter.getShortname());
        assertEquals(FilterType.NUMERIC, filter.getType());
        assertEquals(Currency.RUR.name(), filter.getUnit());

        assertEquals(NumericFilter.class, filter.getClass());
        NumericFilter price = (NumericFilter) filter;
        assertFalse(price.getExactly());
        assertEquals("58580", price.getMinValue());
        assertEquals("71490", price.getMaxValue());
    }

    private void checkCpu(Map<String, SimpleFilter> filters) {
        SimpleFilter filter = filters.get("5085105");
        assertNotNull(filter);
        assertEquals("Частота процессора", filter.getName());
        assertEquals(FilterType.NUMERIC, filter.getType());
        assertEquals("МГц", filter.getUnit());

        assertEquals(NumericFilter.class, filter.getClass());
        NumericFilter cpu = (NumericFilter) filter;
        assertTrue(cpu.getExactly());
        assertEquals("1100", cpu.getMinValue());
        assertEquals("1100", cpu.getMaxValue());
    }

    private void checkWiFi(Map<String, SimpleFilter> filters) {
        SimpleFilter filter = filters.get("5085139");
        assertNotNull(filter);
        assertEquals("Wi-Fi", filter.getName());
        assertEquals(FilterType.BOOL, filter.getType());
        assertEquals(BoolFilter.class, filter.getClass());
    }

    private void checkDelivery(Map<String, SimpleFilter> filters) {
        SimpleFilter filter = filters.get(Filters.DELIVERY_INTERVAL_FILTER_CODE);
        assertNotNull(filter);
        assertEquals("Срок доставки курьером", filter.getName());
        assertEquals(FilterType.ENUMERATOR, filter.getType());

        assertEquals(EnumFilter.class, filter.getClass());
        EnumFilter delivery = (EnumFilter) filter;
        assertEquals(EnumFilterType.GENERIC, delivery.getEnumFilterType());

        List<? extends EnumValue> options = delivery.getOptions();
        assertNotNull(options);
        assertEquals(3, options.size());
        assertEquals("0", options.get(0).getValueId());
        assertEquals("Сегодня", options.get(0).getValueText());

        assertEquals("1", options.get(1).getValueId());
        assertEquals("Завтра", options.get(1).getValueText());

        assertEquals("5", options.get(2).getValueId());
        assertEquals("До 5 дней", options.get(2).getValueText());
    }

    private void checkShops(Map<String, SimpleFilter> filters) {
        SimpleFilter filter = filters.get(Filters.SHOP_FILTER_CODE);
        assertNotNull(filter);
        assertEquals("Магазины", filter.getName());
        assertEquals("shop", filter.getShortname());
        assertEquals(FilterType.ENUMERATOR, filter.getType());
        assertEquals(FilterSubType.MULTI_CHOICE, filter.getSubType());

        assertEquals(EnumFilter.class, filter.getClass());
        EnumFilter shops = (EnumFilter) filter;
        assertEquals(EnumFilterType.SHOP, shops.getEnumFilterType());

        List<? extends EnumValue> options = shops.getOptions();
        assertNotNull(options);
        assertEquals(2, options.size());

        assertEquals("13063", options.get(0).getValueId());
        assertEquals("COMP2YOU", options.get(0).getValueText());
        assertEquals(EnumValueShop.class, options.get(0).getClass());
        assertEquals("1", ((EnumValueShop) options.get(0)).getCount());

        assertEquals("61702", options.get(1).getValueId());
        assertEquals("BigComp.ru", options.get(1).getValueText());
        assertEquals(EnumValueShop.class, options.get(1).getClass());
        assertEquals("3", ((EnumValueShop) options.get(1)).getCount());
    }

    private void checkText(Map<String, SimpleFilter> filters) {
        SimpleFilter filter = filters.get(TextParameters.FILTER_CODE);
        assertNotNull(filter);
        assertEquals("Поиск по фразе", filter.getName());
        assertEquals("text", filter.getShortname());
        assertEquals(FilterType.TEXT, filter.getType());
    }

}
