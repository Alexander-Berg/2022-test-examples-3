package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.parameterizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BsHrefParametrizingServiceTest {
    private ParamConverter paramConverter;
    private BsHrefParametrizingService parametrizingService;

    @BeforeEach
    void before() {
        paramConverter = mock(ParamConverter.class);
        this.parametrizingService = new BsHrefParametrizingService(paramConverter);
    }

    @Test
    void substitutionTest() {
        var href = "http://deliveryfm.ru/edible/?utm_source={test_substitution}";
        var replacingParams = mock(ReplacingParams.class);

        when(paramConverter.convert(eq("test_substitution"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.substitution("result"));

        var result = parametrizingService.parameterize(href, replacingParams);
        assertThat(result).isEqualTo("http://deliveryfm.ru/edible/?utm_source=result");
    }

    @Test
    void substitutionUrlEncodeTest() {
        var href = "http://deliveryfm.ru/edible/?utm_source={test_substitution}";
        var replacingParams = mock(ReplacingParams.class);

        when(paramConverter.convert(eq("test_substitution"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.substitution("Строка на русском языке с символами @+%/ ~ ? *()<>[]!,"));

        var result = parametrizingService.parameterize(href, replacingParams);
        assertThat(result).isEqualTo("http://deliveryfm.ru/edible/?utm_source=%D0%A1%D1%82%D1%80%D0%BE%D0%BA%D0%B0%20" +
                "%D0%BD%D0%B0%20%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%BE%D0%BC%20%D1%8F%D0%B7%D1%8B%D0%BA%D0%B5%20%D1%81" +
                "%20%D1%81%D0%B8%D0%BC%D0%B2%D0%BE%D0%BB%D0%B0%D0%BC%D0%B8%20%40%2B%25%2F%20~%20%3F%20%2A%28%29%3C%3E" +
                "%5B%5D%21%2C");
    }

    @Test
    void macroTest() {
        var href = "http://deliveryfm.ru/edible/?utm_source={test_macro}";
        var replacingParams = mock(ReplacingParams.class);

        when(paramConverter.convert(eq("test_macro"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.macro("TEST_MACRO"));

        var result = parametrizingService.parameterize(href, replacingParams);
        assertThat(result).isEqualTo("http://deliveryfm.ru/edible/?utm_source={TEST_MACRO}");
    }

    @Test
    void unknownParameterTest() {
        var href = "http://deliveryfm.ru/edible/?utm_source={unknown_parameter}";
        var replacingParams = mock(ReplacingParams.class);

        when(paramConverter.convert(eq("unknown_parameter"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.notFound());

        var result = parametrizingService.parameterize(href, replacingParams);
        assertThat(result).isEqualTo("http://deliveryfm.ru/edible/?utm_source={unknown_parameter}");
    }

    @Test
    void severalParametersTest() {
        var href = "http://deliveryfm.ru/edible/?utm_source={param1}&abc={param2}&cde={param3}&fgt={param4}";
        var replacingParams = mock(ReplacingParams.class);

        when(paramConverter.convert(eq("param1"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.substitution("result1"));
        when(paramConverter.convert(eq("param2"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.macro("PARAM"));
        when(paramConverter.convert(eq("param3"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.substitution("result3"));
        when(paramConverter.convert(eq("param4"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.notFound());

        var result = parametrizingService.parameterize(href, replacingParams);
        assertThat(result).isEqualTo("http://deliveryfm.ru/edible/?utm_source=result1&abc={PARAM}&cde=result3&fgt" +
                "={param4}");
    }

    /**
     * Тест проверяет, что если в ссылке есть символы регулярных выражений, то они не будут влиять на результат
     */
    @Test
    void regExpSymbolsInHrefTest() {
        var href = "http://r.mail.ru/n256807638?_1larg_sub=$subId";
        var params = mock(ReplacingParams.class);
        var result = parametrizingService.parameterize(href, params);
        assertThat(result).isEqualTo("http://r.mail.ru/n256807638?_1larg_sub=$subId");

    }

    @Test
    void brokenBracesTest() {
        var hrefTail = "campaign={campaign_name&campaign_lat={campaign_name_lat}";
        var params = mock(ReplacingParams.class);
        when(paramConverter.convert(eq("campaign_name"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.substitution("Кампания"));
        when(paramConverter.convert(eq("campaign_name_lat"), any(ReplacingParams.class)))
                .thenReturn(ConversionResult.substitution("Campaniya"));
        var result = parametrizingService.parameterize(hrefTail, params);
        assertThat(result).isEqualTo("campaign={campaign_name&campaign_lat=Campaniya");
    }

    @Test
    void nestedBracesTest() {
        var hrefTail = "campaign={id:{campaign_id},name:{campaign_name}}";
        var params = mock(ReplacingParams.class);
        when(paramConverter.convert(any(), any()))
                .thenReturn(ConversionResult.substitution("TEST"));
        var result = parametrizingService.parameterize(hrefTail, params);
        assertThat(result).isEqualTo("campaign={id:TEST,name:TEST}");
    }
}
