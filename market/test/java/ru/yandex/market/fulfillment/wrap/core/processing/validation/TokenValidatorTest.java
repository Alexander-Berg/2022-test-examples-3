package ru.yandex.market.fulfillment.wrap.core.processing.validation;

import com.google.common.collect.ArrayListMultimap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.api.RequestType;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

class TokenValidatorTest extends SoftAssertionSupport {

    private TokenValidator tokenValidator = new TokenValidator(this::getWrapperConfigMultimap);

    @Test
    void failToCreateTokenValidatorWithNullMap() {
        tokenValidator = new TokenValidator(() -> null);
        softly.assertThatThrownBy(
            () -> tokenValidator.assertIsValid(new Token("wrapperToken"), RequestType.GET_STOCKS)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failToCreateTokenValidatorWithEmptyMap() {
        tokenValidator = new TokenValidator(ArrayListMultimap::create);
        softly.assertThatThrownBy(
            () -> tokenValidator.assertIsValid(new Token("wrapperToken"), RequestType.GET_STOCKS)
        ).isInstanceOf(FulfillmentApiException.class);
    }

    @Test
    void concreteValidateSuccess() {
        tokenValidator.assertIsValid(new Token("wrapperToken"), RequestType.GET_STOCKS);
    }

    @Test
    void concreteValidateFail() {
        softly.assertThatThrownBy(
            () -> tokenValidator.assertIsValid(new Token("wrapperToken"), RequestType.CANCEL_INBOUND)
        ).isInstanceOf(FulfillmentApiException.class);
    }

    private ArrayListMultimap<Token, RequestType> getWrapperConfigMultimap() {
        ArrayListMultimap<Token, RequestType> wrapperConfigMultiMap = ArrayListMultimap.create();
        wrapperConfigMultiMap.put(new Token("wrapperToken"), RequestType.GET_STOCKS);
        return wrapperConfigMultiMap;
    }

}
