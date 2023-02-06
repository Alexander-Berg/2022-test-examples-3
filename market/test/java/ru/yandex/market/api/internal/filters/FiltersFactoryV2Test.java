package ru.yandex.market.api.internal.filters;

import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.market.api.error.ValidationErrors;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class FiltersFactoryV2Test {

    @Test
    public void shouldExtractShippingFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.SHIPPING_FILTER_CODE, "pickup");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        errors.throwIfHasErrors();
        assertEquals("pickup", filters.get(Filters.SHIPPING_FILTER_CODE));
    }

    @Test
    public void shouldAddErrorIfCannotExtractShippingFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.SHIPPING_FILTER_CODE, "wrong-code");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        assertTrue(filters.isEmpty());

        assertEquals(1, errors.size());
        assertThat(errors.getErrors().get(0).getMessage(), both(startsWith("Parameter '-10' format is incorrect. " +
                "Unknown value 'wrong-code'"))
                .and(containsString("ALL"))
                .and(containsString("DELIVERY"))
                .and(containsString("PICKUP"))
                .and(containsString("POSTOMAT"))
                .and(containsString("STORE"))
        );
    }

    @Test
    public void shouldExtractShopRatingFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.SHOP_RATING_FILTER_CODE, "4");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        errors.throwIfHasErrors();
        assertEquals("4", filters.get(Filters.SHOP_RATING_FILTER_CODE));
    }

    @Test
    public void shouldAddErrorIfCannotExtractShopRatingFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.SHOP_RATING_FILTER_CODE, "-2");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        assertTrue(filters.isEmpty());

        assertEquals(1, errors.size());
        assertEquals(
            "Parameter '-5' has invalid value. Parameter does not fit range constraint (actual value = -2, min value = 0, max value = 5)",
            errors.getErrors().get(0).getMessage()
        );
    }

    @Test
    public void shouldExtractNumberFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.PRICE_FILTER_CODE, "100~400");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        errors.throwIfHasErrors();
        assertEquals("100~400", filters.get(Filters.PRICE_FILTER_CODE));
    }

    @Test
    public void shouldAddErrorIfCannotExtractNumberFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.PRICE_FILTER_CODE, "100");

        ValidationErrors errors = new ValidationErrors();

        FiltersFactoryV2.extractFilters(request, errors);

        assertEquals(1, errors.size());
        assertEquals(
            "Parameter '-1' value is incorrect",
            errors.getErrors().get(0).getMessage()
        );
    }

    @Test
    public void shouldExtractBooleanFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.WARRANTY_FILTER_CODE, "TRUE");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        errors.throwIfHasErrors();
        assertEquals("1", filters.get(Filters.WARRANTY_FILTER_CODE));
    }

    @Test
    public void shouldAddErrorIfCannotExtractBooleanFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.WARRANTY_FILTER_CODE, "100");

        ValidationErrors errors = new ValidationErrors();

        FiltersFactoryV2.extractFilters(request, errors);

        assertEquals(1, errors.size());
        assertEquals(
            "Parameter '-2' value is incorrect",
            errors.getErrors().get(0).getMessage()
        );
    }

    @Test
    public void shouldExtractEnumFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.SHOP_FILTER_CODE, "-100,200,300,-400");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        errors.throwIfHasErrors();
        assertEquals("-100,200,300,-400", filters.get(Filters.SHOP_FILTER_CODE));
    }

    @Test
    public void shouldAddErrorIfCannotExtractEnumFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(Filters.SHOP_FILTER_CODE, "Array");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        assertTrue(filters.isEmpty());

        assertEquals(1, errors.size());
        assertEquals(
            "Parameter '-6' format is incorrect. Expected format: integer number",
            errors.getErrors().get(0).getMessage()
        );
    }

    @Test
    public void shouldExtractGlFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("5085105", "100~4000");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        errors.throwIfHasErrors();
        assertEquals("100~4000", filters.get("5085105"));
    }

    @Test
    public void shouldAddErrorIfCannotExtractGlFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("5085105", "Array");

        ValidationErrors errors = new ValidationErrors();

        Map<String, String> filters = FiltersFactoryV2.extractFilters(request, errors);

        assertTrue(filters.isEmpty());

        assertEquals(1, errors.size());
        assertEquals(
            "Parameter '5085105' value is incorrect",
            errors.getErrors().get(0).getMessage()
        );
    }
}
