package ru.yandex.direct.api.v5.ws;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(value = Parameterized.class)
public class ApiLocaleResolverTest {
    private static final String DEFAULT_LOCALE_STR = "en";
    private static final String SUPPORTED_LOCALE_STR = "ru";

    private ApiLocaleResolver resolverUnderTest;

    @Parameterized.Parameters(name = "for ''{0}'' should return ''{1}''")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"xxx", DEFAULT_LOCALE_STR},
                {DEFAULT_LOCALE_STR, DEFAULT_LOCALE_STR},
                {SUPPORTED_LOCALE_STR, SUPPORTED_LOCALE_STR},
        };
        return Arrays.asList(data);
    }

    @Parameterized.Parameter(0)
    public String sourceLocaleString;

    @Parameterized.Parameter(1)
    public String expectedLocaleString;

    @Before
    public void init() {
        resolverUnderTest = new ApiLocaleResolver(
                new Locale(DEFAULT_LOCALE_STR),
                ImmutableSet.of(new Locale(DEFAULT_LOCALE_STR), new Locale(SUPPORTED_LOCALE_STR)));
    }

    @Test
    public void resolveLocale() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getLocale()).thenReturn(new Locale(sourceLocaleString));
        assertThat(resolverUnderTest.resolveLocale(request)).isEqualTo(new Locale(expectedLocaleString));
    }

}
