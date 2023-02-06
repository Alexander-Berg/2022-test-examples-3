package ru.yandex.market.checkout.checkouter.json.validation;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.validation.LimitMaxAmountResult;
import ru.yandex.market.checkout.checkouter.validation.PromoCodeValidationResult;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class ValidationResultJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void promoCodeSerialize() throws Exception {
        PromoCodeValidationResult promoCodeValidationResult = new PromoCodeValidationResult(
                "promoCode", "errorCode", ValidationResult.Severity.ERROR, "userMessage"
        );

        String json = write(promoCodeValidationResult);
        System.out.println(json);

        checkJson(json, "$." + Names.ValidationResult.PROMOCODE, "promoCode");
        checkJson(json, "$." + Names.ValidationResult.TYPE, "PROMO_CODE_ERROR");
        checkJson(json, "$." + Names.ValidationResult.CODE, "errorCode");
        checkJson(json, "$." + Names.ValidationResult.SEVERITY, ValidationResult.Severity.ERROR.name());
        checkJson(json, "$." + Names.ValidationResult.USER_MESSAGE, "userMessage");
    }

    @Test
    public void promoCodeDeserialize() throws Exception {
        String json = "{\"type\":\"PROMO_CODE_ERROR\",\"code\":\"code\",\"severity\":\"ERROR\"," +
                "\"userMessage\":\"userMessage\"}";

        ValidationResult validationResult = read(ValidationResult.class, json);
        assertThat(validationResult, CoreMatchers.instanceOf(PromoCodeValidationResult.class));

        PromoCodeValidationResult promoCodeValidationResult = (PromoCodeValidationResult) validationResult;
        Assertions.assertEquals("code", validationResult.getCode());
        Assertions.assertEquals(ValidationResult.Severity.ERROR, validationResult.getSeverity());
        Assertions.assertEquals("userMessage", promoCodeValidationResult.getUserMessage());
    }

    @Test
    public void limitMaxAmountSerialize() throws Exception {
        LimitMaxAmountResult limitMaxAmountResult = new LimitMaxAmountResult(
                "code", ValidationResult.Severity.ERROR, new BigDecimal("15000.12"), Currency.RUR
        );

        String json = write(limitMaxAmountResult);
        System.out.println(json);

        checkJson(json, "$." + Names.ValidationResult.TYPE, "limitMaxAmount");
        checkJson(json, "$." + Names.ValidationResult.CODE, "code");
        checkJson(json, "$." + Names.ValidationResult.SEVERITY, ValidationResult.Severity.ERROR.name());
        checkJson(json, "$." + Names.LimitMaxAmount.EXT_MAX_AMOUNT, 15000.12);
        checkJson(json, "$." + Names.LimitMaxAmount.EXT_MAX_AMOUNT_CURRENCY, Currency.RUR.name());
    }

    @Test
    public void limitMaxAmountDeserialize() throws IOException {
        String json = "{\"type\":\"limitMaxAmount\",\"code\":\"code\",\"severity\":\"ERROR\",\"extMaxAmount\":15000" +
                ".12,\"extMaxAmountCurrency\":\"RUR\"}\n";

        ValidationResult validationResult = read(ValidationResult.class, json);
        assertThat(validationResult, CoreMatchers.instanceOf(LimitMaxAmountResult.class));

        LimitMaxAmountResult limitMaxAmountResult = (LimitMaxAmountResult) validationResult;

        Assertions.assertEquals("code", limitMaxAmountResult.getCode());
        Assertions.assertEquals(ValidationResult.Severity.ERROR, limitMaxAmountResult.getSeverity());
        Assertions.assertEquals(new BigDecimal("15000.12"), limitMaxAmountResult.getMaxAmount());
        Assertions.assertEquals(Currency.RUR, limitMaxAmountResult.getCurrency());
    }

    @Test
    public void defaultSerialize() throws IOException, ParseException {
        ValidationResult validationResult = new ValidationResult("code", ValidationResult.Severity.ERROR);

        String json = write(validationResult);
        System.out.println(json);

        checkJson(json, "$." + Names.ValidationResult.TYPE, "basic");
        checkJson(json, "$." + Names.ValidationResult.CODE, "code");
        checkJson(json, "$." + Names.ValidationResult.SEVERITY, ValidationResult.Severity.ERROR.name());
    }

    @Test
    public void defaultDeserialize() throws IOException {
        String json = "{\"type\":\"basic\",\"code\":\"code\",\"severity\":\"ERROR\"}";

        ValidationResult validationResult = read(ValidationResult.class, json);
        assertThat(validationResult, instanceOf(ValidationResult.class));
        Assertions.assertEquals("code", validationResult.getCode());
        Assertions.assertEquals(ValidationResult.Severity.ERROR, validationResult.getSeverity());
    }
}
