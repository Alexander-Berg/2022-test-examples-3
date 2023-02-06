package ru.yandex.direct.api.v5.entity.clients.validation;

import com.yandex.direct.api.v5.clients.UpdateRequest;
import com.yandex.direct.api.v5.generalclients.ClientUpdateItem;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class UpdateRequestValidatorTest {
    private UpdateRequestValidator validatorUnderTest;

    @Before
    public void setUp() {
        validatorUnderTest = new UpdateRequestValidator();
    }

    @Test
    public void validateRequest_oneItem() throws Exception {
        ValidationResult<UpdateRequest, DefectType> vr = validatorUnderTest.validate(
                new UpdateRequest().withClients(new ClientUpdateItem()));
        assertThat(vr.flattenErrors(), empty());
    }

    @Test
    public void validateRequest_tooManyItems() throws Exception {
        ValidationResult<UpdateRequest, DefectType> vr = validatorUnderTest.validate(
                new UpdateRequest().withClients(new ClientUpdateItem(), new ClientUpdateItem()));
        assumeThat(vr.flattenErrors(), hasSize(1));
        assertThat(vr.flattenErrors(),
                contains(validationError(path(field("Clients")), DefectTypes.maxCollectionSize(1))));
    }
}
