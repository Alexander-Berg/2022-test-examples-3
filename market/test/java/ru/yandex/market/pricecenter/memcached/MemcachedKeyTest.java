package ru.yandex.market.pricecenter.memcached;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.MemcachedClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

import ru.yandex.market.pricecenter.core.dao.CostsAndSalesClickhouseService;
import ru.yandex.market.pricecenter.core.entity.report.costsandsales.CalculationType;
import ru.yandex.market.pricecenter.core.entity.report.costsandsales.CostAndSalesParam;
import ru.yandex.market.pricecenter.core.entity.report.costsandsales.Currency;
import ru.yandex.market.pricecenter.core.entity.report.costsandsales.CurrencyEntity;
import ru.yandex.market.pricecenter.core.entity.report.costsandsales.diagrams.DiagramGrouping;
import ru.yandex.market.pricecenter.core.internal.CacheName;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemcachedKeyTest {

    private static final KeyGenerator defaultKeyGenerator = new SimpleKeyGenerator();

    private final CostsAndSalesClickhouseService costsAndSalesClickhouseService = mock(CostsAndSalesClickhouseService.class);

    private final KeyGenWrapper offers = new KeyGenWrapper(new MemcachedKeyGenerator.Offers());
    private final KeyGenWrapper categories = new KeyGenWrapper(new MemcachedKeyGenerator.Categories());
    private final KeyGenWrapper diagram = new KeyGenWrapper(new MemcachedKeyGenerator.Diagram());

    @RequiredArgsConstructor
    @Getter
    static class KeyGenWrapper {
        private final KeyGenerator keyGenerator;
        private Object defaultKey;
        private Object customKey;

        void generateKey(InvocationOnMock invocation) {
            customKey = generateKey(invocation, keyGenerator);
            defaultKey = generateKey(invocation, defaultKeyGenerator);
        }

        static Object generateKey(InvocationOnMock invocation, KeyGenerator keyGen) {
            return keyGen.generate(invocation.getMock(), invocation.getMethod(), invocation.getArguments());
        }
    }

    private <T> Answer<T> answer(KeyGenWrapper wrapper) {
        return invocation -> {
            wrapper.generateKey(invocation);
            return null;
        };
    }

    @Before
    public void setUp() throws IOException {
        when(costsAndSalesClickhouseService.getOffers(anyLong(), any(Date.class), any(Date.class), anyList(), anyInt(),
                any(CalculationType.class), anyDouble(), anyString(), anyInt(), anyInt(), anyBoolean())).then(answer(offers));
        when(costsAndSalesClickhouseService.getCategories(anyLong(), any(Date.class), any(Date.class), anyList()))
                .then(answer(categories));
        when(costsAndSalesClickhouseService.getDiagramForItems(anyLong(), any(LocalDate.class), any(LocalDate.class),
                anyList(), anyList(), any(DiagramGrouping.class), any(CalculationType.class), anyList(),
                any(CurrencyEntity.class), anyDouble(), anyBoolean()))
                .then(answer(diagram));
    }

    @Test
    public void compressOffers() {
        final List<String> offers = Arrays.asList(
                "566102:10007196:128673716",
                "566102:10007196:130757669",
                "566102:10007196:127324103",
                "566102:10007196:129961348",
                "566102:10007196:130645731",
                "566102:10007196:133018509",
                "566102:10007196:131560496");
        String compressed = MemcachedKeyGenerator.compressTriple(offers);
        assertThat(compressed, is("{566102:{10007196:[127324103,128673716,129961348,130645731,130757669,131560496,133018509]}}"));
        assertThat(String.join(",", offers).length(), greaterThan(compressed.length()));
    }

    @Test
    public void compressOffers2() {
        final List<String> offers = Arrays.asList(
                "566102:10007196:128673716",
                "566102:10007196:130757669",
                "568102:10007196:127324103",
                "566102:10007196:129961348",
                "566102:10007197:130645731",
                "564102:10007196:133018509",
                "566102:10007196:131560496");
        String compressed = MemcachedKeyGenerator.compressTriple(offers);
        assertThat(compressed, is("{564102:{10007196:[133018509]},566102:{10007196:[128673716,129961348,130757669,131560496],10007197:[130645731]},568102:{10007196:[127324103]}}"));
        assertThat(String.join(",", offers).length(), greaterThan(compressed.length()));
    }

    @Test
    public void diagramForItems() {
        costsAndSalesClickhouseService.getDiagramForItems(21550911L, LocalDate.parse("2019-03-12"),
                LocalDate.parse("2019-03-19"), Arrays.asList(
                        "566102:10007196:128673716",
                        "566102:10007196:130757669",
                        "568102:10007196:127324103",
                        "566102:10007196:129961348",
                        "566102:10007197:130645731",
                        "564102:10007196:133018509",
                        "566102:10007196:131560496"),
                Collections.singletonList(CostAndSalesParam.click_count), DiagramGrouping.DAY, CalculationType.AVERAGE,
                Collections.emptyList(),
                Currency.RUR, 1.0D, false);
        assertKeyGenerator(CacheName.DISTRIBUTED_COSTS_AND_SALES, diagram);

    }

    @Test
    public void categories() throws IOException {
        costsAndSalesClickhouseService.getCategories(21550911L, Date.valueOf(LocalDate.parse("2019-03-12")),
                Date.valueOf(LocalDate.parse("2019-03-19")), Arrays.asList(
                        "556782:1000560000000000",
                        "556782:1000530000000000",
                        "556782:1000520000000000",
                        "556782:1000540000000000",
                        "556782:1000560000000000",
                        "568724:1000510000000000",
                        "568724:1000520000000000",
                        "556782:1000550000000000"));
        assertKeyGenerator(CacheName.DISTRIBUTED_COSTS_AND_SALES, categories);
        assertThat(categories.customKey.toString(), not(containsString("root")));
    }

    @Test
    public void categoriesWithRoot() throws IOException {
        costsAndSalesClickhouseService.getCategories(21550911L, Date.valueOf("2019-03-04"),
                Date.valueOf("2019-03-19"),
                Arrays.asList(
                        "556782:1000560000000000",
                        "556782:1000530000000000",
                        "root",
                        "556782:1000520000000000",
                        "556782:1000540000000000",
                        "556782:1000560000000000",
                        "568724:1000510000000000",
                        "568724:1000520000000000",
                        "556782:1000550000000000"));
        assertKeyGenerator(CacheName.DISTRIBUTED_COSTS_AND_SALES, categories);
        assertThat(categories.customKey.toString(), containsString("21550911_2019-03-04_15_root{"));
    }

    @Test
    public void offers() {
        costsAndSalesClickhouseService.getOffers(21550911L, Date.valueOf(LocalDate.parse("2019-03-12")),
                Date.valueOf(LocalDate.parse("2019-03-19")), Arrays.asList(
                        "566102:10007196:128673716",
                        "566102:10007196:130757669",
                        "568102:10007196:127324103",
                        "566102:10007196:129961348",
                        "566102:10007197:130645731",
                        "564102:10007196:133018509",
                        "566102:10007196:131560496"), 1, CalculationType.ECOMMERCE, 1.1D, "sort-order", 1, 10, true);
        assertKeyGenerator(CacheName.DISTRIBUTED_COSTS_AND_SALES, offers);
    }

    private void assertKeyGenerator(String cacheName, KeyGenWrapper keyGenWrapper) {
        String keyPrefix = keyPrefix(cacheName);

        final String fullKey = keyPrefix + ":" + keyGenWrapper.defaultKey;
        assertThat(fullKey.length(), greaterThan(MemcachedClient.MAX_KEY_LENGTH));

        final Object customKey = keyGenWrapper.customKey;
        assertThat(customKey, is(notNullValue()));
        final String newFullKey = keyPrefix + ":" + customKey;
        assertThat(newFullKey.length(), lessThanOrEqualTo(MemcachedClient.MAX_KEY_LENGTH));
    }

    private String keyPrefix(String cacheName) {
        return "pc-prd:" + cacheName + ":" + System.currentTimeMillis();
    }
}
