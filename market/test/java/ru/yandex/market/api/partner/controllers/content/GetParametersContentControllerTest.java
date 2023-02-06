package ru.yandex.market.api.partner.controllers.content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mbi.util.io.MbiFiles;

/**
 * Тесты для {@link ContentController}.
 */
@ParametersAreNonnullByDefault
class GetParametersContentControllerTest extends FunctionalTest {
    private static final long CATEGORY_ID = 34579;
    private static final PartnerContentApi.GetParametersResponse GENERIC_IR_GET_PARAMETERS_RESPONSE =
            genericIrGetParametersResponse();

    @Autowired
    private PartnerContentService marketProtoPartnerContentService;

    @Nonnull
    private static PartnerContentApi.GetParametersResponse genericIrGetParametersResponse() {
        return PartnerContentApi.GetParametersResponse.newBuilder()
                .addModelParameter(PartnerContentApi.Parameter.newBuilder()
                        .setId(2357250)
                        .setName("Размер")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setImportant(true)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build())
                .addModelParameter(PartnerContentApi.Parameter.newBuilder()
                        .setId(3562825)
                        .setName("Цвет")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.STRING)
                                .build())
                        .build())
                .addSkuDefiningParameter(PartnerContentApi.Parameter.newBuilder()
                        .setId(2835628)
                        .setName("Название")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setImportant(true)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.NUMERIC)
                                .setUnit("kg")
                                .setNumericDomain(PartnerContentApi.NumericDomain.newBuilder()
                                        .setMinValue(0.25)
                                        .setMaxValue(1024.25)
                                        .build())
                                .build())
                        .build())
                .addSkuDefiningParameter(PartnerContentApi.Parameter.newBuilder()
                        .setId(3456739)
                        .setName("Вибрация")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.ENUM)
                                .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                        .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                .setId(1234154)
                                                .setName("Samsung")
                                                .build())
                                        .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                .setId(2864378)
                                                .setName("Apple")
                                                .build())
                                        .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                .setId(2547634)
                                                .setName("Asus")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .addSkuInformationalParameter(PartnerContentApi.Parameter.newBuilder()
                        .setId(8935629)
                        .setName("Серия")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.DEPENDENT_ENUM)
                                .setUnit("mm")
                                .setDependentEnumDomain(PartnerContentApi.DependentEnumDomain.newBuilder()
                                        .addAllDependsOnParamId(Arrays.asList(123412L, 1462144L))
                                        .addDependentSubdomain(PartnerContentApi.DependentEnumSubdomain.newBuilder()
                                                .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                        .setParamId(123412L)
                                                        .setType(PartnerContentApi.ParameterType.ENUM)
                                                        .setEnumOptionId(39457)
                                                        .build())
                                                .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                        .setParamId(1462144L)
                                                        .setType(PartnerContentApi.ParameterType.ENUM)
                                                        .setEnumOptionId(563458)
                                                        .build())
                                                .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                                        .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                                .setId(349867)
                                                                .setName("1/2")
                                                                .build())
                                                        .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                                .setId(345698)
                                                                .setName("1/4")
                                                                .build())
                                                        .build())
                                                .build())
                                        .addDependentSubdomain(PartnerContentApi.DependentEnumSubdomain.newBuilder()
                                                .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                        .setParamId(123412L)
                                                        .setType(PartnerContentApi.ParameterType.ENUM)
                                                        .setEnumOptionId(347274)
                                                        .build())
                                                .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                        .setParamId(1462144L)
                                                        .setType(PartnerContentApi.ParameterType.ENUM)
                                                        .setEnumOptionId(283648)
                                                        .build())
                                                .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                                        .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                                .setId(2389462)
                                                                .setName("3/2")
                                                                .build())
                                                        .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                                .setId(2374298)
                                                                .setName("3/4")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    void testGetCategoryContentStructureJson() throws IOException {
        Mockito.when(marketProtoPartnerContentService.getParameters(Mockito.any()))
                .thenReturn(GENERIC_IR_GET_PARAMETERS_RESPONSE);
        String url = String.format("%s/categories/%d/parameters", urlBasePrefix, CATEGORY_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetCategoryContentStructure.json"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService)
                .getParameters(ArgumentMatchers.argThat(request -> request.getCategoryId() == CATEGORY_ID));

    }

    @Test
    void testGetCategoryContentStructureXml() throws IOException {
        Mockito.when(marketProtoPartnerContentService.getParameters(Mockito.any()))
                .thenReturn(GENERIC_IR_GET_PARAMETERS_RESPONSE);
        String url = String.format("%s/categories/%d/parameters", urlBasePrefix, CATEGORY_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetCategoryContentStructure.xml"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.xmlEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService)
                .getParameters(ArgumentMatchers.argThat(request -> request.getCategoryId() == CATEGORY_ID));

    }

    @Test
    void testGetCategoryContentStructurePassThroughErrorXml() {
        Mockito.when(marketProtoPartnerContentService.getParameters(Mockito.any()))
                .thenReturn(PartnerContentApi.GetParametersResponse.newBuilder()
                        .setStatus(PartnerContentApi.GetParametersResponse.Status.NOT_LEAF_CATEGORY)
                        .setErrorMessage("Не листовая категория")
                        .build());
        String url = String.format("%s/categories/%d/parameters", urlBasePrefix, CATEGORY_ID);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML)
        );
        exception.printStackTrace(System.out);

        //language=xml
        String expected = "" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code='INVALID_CATEGORY' message='NOT_LEAF_CATEGORY: Не листовая категория'/> " +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(MbiMatchers.xmlEquals(expected))
                )
        );
    }

    @Test
    void testGetCategoryContentStructurePassThroughErrorJson() {
        Mockito.when(marketProtoPartnerContentService.getParameters(Mockito.any()))
                .thenReturn(PartnerContentApi.GetParametersResponse.newBuilder()
                        .setStatus(PartnerContentApi.GetParametersResponse.Status.NOT_LEAF_CATEGORY)
                        .setErrorMessage("Не листовая категория")
                        .build());
        String url = String.format("%s/categories/%d/parameters", urlBasePrefix, CATEGORY_ID);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON)
        );
        exception.printStackTrace(System.out);

        //language=json
        String expected = ""
                + "{"
                + "    \"status\":\"ERROR\","
                + "    \"errors\":["
                + "        {"
                + "            \"code\":\"INVALID_CATEGORY\","
                + "            \"message\":\"NOT_LEAF_CATEGORY: Не листовая категория\""
                + "        }"
                + "    ]"
                + "}";
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(MbiMatchers.jsonEquals(expected))
                )
        );
    }
}
