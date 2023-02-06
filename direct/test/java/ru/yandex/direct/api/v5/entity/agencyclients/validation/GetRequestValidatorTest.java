package ru.yandex.direct.api.v5.entity.agencyclients.validation;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.agencyclients.AgencyClientFieldEnum;
import com.yandex.direct.api.v5.agencyclients.AgencyClientsSelectionCriteria;
import com.yandex.direct.api.v5.agencyclients.GetRequest;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
public class GetRequestValidatorTest {
    @Test
    public void validate_nullLogin_errorIsGenerated() {
        GetRequest request = new GetRequest()
                .withSelectionCriteria(new AgencyClientsSelectionCriteria().withLogins(Collections.singleton(null)))
                .withFieldNames(AgencyClientFieldEnum.LOGIN);
        ValidationResult<GetRequest, DefectType> validation = GetRequestValidator.validateRequest(request);

        assertThat(validation).is(matchedBy(hasDefectWith(validationError(8000))));
    }
}
