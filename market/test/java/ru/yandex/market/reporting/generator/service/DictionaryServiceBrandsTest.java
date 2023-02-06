package ru.yandex.market.reporting.generator.service;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.reporting.generator.dao.ClickhouseService;
import ru.yandex.market.reporting.generator.domain.Brand;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nettoyeur
 * @since 24.05.2017
 */
public class DictionaryServiceBrandsTest {

    private static final Brand РОМАШКА = new Brand(1, "Ромашка");
    private static final Brand СНЕЖИНКА_2 = new Brand(2, "Снежинка");
    private static final Brand СНЕЖИНКА_3 = new Brand(3, "Снежинка");
    private static final Brand ЯГОДКА = new Brand(10, "Ягодка");

    private DictionaryService dictionaryService;

    @Before
    public void setUp() throws Exception {
        dictionaryService = dictionaryService();
    }

    @Test
    public void getBrands() throws Exception {
        Map<String, List<Brand>> brands = dictionaryService.getBrands();

        assertThat(brands.size(), is(3));
        assertThat(brands.containsKey("Ромашка"), is(true));
        assertThat(brands.containsKey("Снежинка"), is(true));
        assertThat(brands.containsKey("Ягодка"), is(true));

        assertThat(brands, hasEntry("Ромашка", singletonList(РОМАШКА)));
        assertThat(brands, hasEntry("Ягодка", singletonList(ЯГОДКА)));

        assertThat(brands, hasEntry(equalTo("Снежинка"), anything()));
        assertThat(brands.get("Снежинка"), iterableWithSize(2));
        assertThat(brands.get("Снежинка"), hasItems(СНЕЖИНКА_2, СНЕЖИНКА_3));
    }

    @Test
    public void queryUniqueBrands() throws Exception {
        List<Brand> brands = dictionaryService.getBrands("Я", 0).getBrands();

        assertThat(brands, hasSize(1));

        assertThat(brands, hasItems(ЯГОДКА));
    }

    @Test
    public void queryBrands() throws Exception {
        List<Brand> brands = dictionaryService.getBrands("С", 0).getBrands();

        assertThat(brands, hasSize(2));

        assertThat(brands, hasItems(СНЕЖИНКА_2, СНЕЖИНКА_3));
    }

    @Test
    public void queryBrandsWithLimit() throws Exception {
        List<Brand> brands = dictionaryService.getBrands("С", 1).getBrands();

        assertThat(brands, hasSize(1));

        assertThat(brands, anyOf((org.hamcrest.Matcher) hasItem(СНЕЖИНКА_2), (org.hamcrest.Matcher) hasItem(СНЕЖИНКА_3)));
    }

    @Test
    public void queryNoBrands() throws Exception {
        List<Brand> brands = dictionaryService.getBrands("Z", 1).getBrands();

        assertThat(brands, hasSize(0));
    }

    private DictionaryService dictionaryService() {
        return new DictionaryService(clickhouseService());
    }

    private ClickhouseService clickhouseService() {
        return when(mock(ClickhouseService.class).getBrands("", 0))
            .thenReturn(asList(РОМАШКА, СНЕЖИНКА_2, СНЕЖИНКА_3, ЯГОДКА)).getMock();
    }
}
