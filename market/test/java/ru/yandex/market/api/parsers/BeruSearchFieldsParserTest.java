package ru.yandex.market.api.parsers;

import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.controller.v2.BeruSearchControllerV3;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.integration.UnitTestBase;

@RunWith(Parameterized.class)
public class BeruSearchFieldsParserTest extends UnitTestBase {

    private final BeruSearchControllerV3.BeruSearchFieldsParser parser = new BeruSearchControllerV3.BeruSearchFieldsParser();

    private String paramValue;
    private boolean expectedAllowed;

    public BeruSearchFieldsParserTest(String paramValue, boolean expectedAllowed) {
        this.paramValue = paramValue;
        this.expectedAllowed = expectedAllowed;
    }

    @Parameterized.Parameters(name = "{0} as parameter is allowed: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                        {"MODEL_LINK", true},
                        {"MODEL_PRICE", true},
                        {"MoDeL_mEdIa", true},
                        {"model_photos", true},
                        {"MODEL_PRICE,OFFER_DELIVERY,MODEL_PHOTOS", true},
                        {"OFFER_DELIVERY,OFFER_PHOTO,OFFER_CHECKOUT_LINK", true},
                        {null, true},
                        {"", true},
                        {"MODEL_PRICE,MODEL_OFFERSsSsS,MODEL_PHOTOS", false},
                        {"RANDOM_WRONG_TEXT", false},
                        {"ALL", false},
                        {"MODEL_ALL", false},
                        {"OFFER_ALL", false}
                }
        );
    }

    @Test
    public void testRequiredExplicitParam() {
        final HttpServletRequest request = MockRequestBuilder.start()
                .methodName("v2/beru/search")
                .param(Parameters.FIELDS_PARAM_NAME, paramValue)
                .build();

        final Result<Collection<Field>, ValidationError> result = parser.get(request);
        Assert.assertEquals(!expectedAllowed, result.hasError());
    }
}
