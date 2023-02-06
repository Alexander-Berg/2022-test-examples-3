package ru.yandex.market.api.common.currency;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
public class CurrencyServiceTest extends UnitTestBase {

    @Mock
    private CurrencyDataFileSupplier dataSupplier;

    private CurrencyService service;

    @Before
    public void setUp() throws Exception {
        service = new CurrencyService(null, null, new CAPICurrencyDataFileSupplier(dataSupplier));
        when(dataSupplier.get()).thenReturn(new CurrencyData() {{
            putRate(Currency.BYR, Currency.RUR, 10000);
            putRate(Currency.RUR, Currency.BYR, 0.0001);
        }});
        service.reloadCache();
    }

    @Test
    public void shouldConvertCurrencyDown() throws Exception {
        assertEquals("1.23", service.convert(Currency.RUR, Currency.BYR, "12340"));
    }

    @Test
    public void shouldConvertCurrencyUp() throws Exception {
        assertEquals("12300", service.convert(Currency.BYR, Currency.RUR, "1.23"));
    }

    @Test
    public void shouldProcessNullValue() throws Exception {
        assertNull(service.convert(Currency.RUR, Currency.BYR, null));
    }

    @Test
    public void shouldSilentlyProcessIncorrectNumber() throws Exception {
        assertNull(service.convert(Currency.RUR, Currency.BYR, "abc"));
    }
}
