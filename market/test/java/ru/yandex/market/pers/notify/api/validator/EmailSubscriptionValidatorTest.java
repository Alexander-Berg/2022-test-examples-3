package ru.yandex.market.pers.notify.api.validator;

import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.notify.exceptions.ExceptionThrower;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;

import javax.validation.ValidationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         19.09.17
 */
public class EmailSubscriptionValidatorTest {
    @Test
    public void validateChildrenJsonEmpty() throws Exception {
        EmailSubscriptionValidator.validateChildrenJson("");
    }

    @Test
    public void validateChildrenJsonEmptyArray() throws Exception {
        EmailSubscriptionValidator.validateChildrenJson("[]");
    }

    @Test
    public void validateChildrenJsonNoDateOfBirth() throws Exception {
        EmailSubscriptionValidator.validateChildrenJson("[{\"gender\":\"male\"}]");
    }

    @Test
    public void validateChildrenJsonNoGender() throws Exception {
        EmailSubscriptionValidator.validateChildrenJson("[{\"dateOfBirth\":\"2017-01-01\"}]");
    }

    @Test
    public void validateChildrenJsonInvalidDateOfBirth() throws Exception {
        assertThrows(ValidationException.class, () -> {
            EmailSubscriptionValidator.validateChildrenJson("[{\"dateOfBirth\":\"-01-01\"}]");
        });
    }

    @Test
    public void validateChildrenJsonInvalidGender() throws Exception {
        assertThrows(RuntimeException.class, () -> {
            EmailSubscriptionValidator.validateChildrenJson("[{\"gender\":\"invalid\"}]");
        });
    }

    @Test
    public void validateChildrenJsonSimple() throws Exception {
        EmailSubscriptionValidator.validateChildrenJson("[" +
            "{\"gender\":\"male\"}," +
            "{\"dateOfBirth\":\"2017-01-01\"}," +
            "{\"dateOfBirth\":\"1990-12-31\",\"gender\":\"female\"}]");
    }

    @Test
    public void validateNullParams() throws Exception {
        EmailSubscriptionValidator.validateParams(null);
    }

    @Test
    public void validateParamsNoAdsLocation() throws Exception {
        Map<String, String> emptyParams = Collections.emptyMap();
        EmailSubscriptionValidator.validateParams(emptyParams);
    }

    @Test
    public void validateParamsValidAdsLocation() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(EmailSubscriptionParam.PARAM_ADS_LOCATION, "mall");
        EmailSubscriptionValidator.validateParams(params);
    }

    @Test
    public void validateParamsNotValidAdsLocation() throws Exception {
        assertThrows(ExceptionThrower.BadRequestException.class, () -> {
            Map<String, String> params = new HashMap<>();
            params.put(EmailSubscriptionParam.PARAM_ADS_LOCATION, "what?");
            EmailSubscriptionValidator.validateParams(params);
        });
    }

    @Test
    public void validateValidEmail() throws Exception {
        EmailSubscriptionValidator.validateEmail("valter@yandex-team.ru");
        EmailSubscriptionValidator.validateEmail("valter+spam@yandex-team.ru");
        EmailSubscriptionValidator.validateEmail("valter@yandex-team.com.gov");
        EmailSubscriptionValidator.validateEmail("v@gov.ru");
        EmailSubscriptionValidator.validateEmail("mail@from-some.store");
    }

    @Test
    public void validateNotValidEmail() throws Exception {
        assertThrows(ValidationException.class, () -> {
            EmailSubscriptionValidator.validateEmail("valter+spam-team.ru@");
        });
    }
}
