package ru.yandex.direct.api.v5.entity.agencyclients.validation;

import java.util.Collections;

import com.yandex.direct.api.v5.agencyclients.AgencyClientUpdateItem;
import com.yandex.direct.api.v5.agencyclients.UpdateRequest;
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

public class UpdateAgencyClientValidatorRequestTest {
    private UpdateAgencyClientRequestValidator validatorUnderTest;
    private static final int MAX_AMOUNT_OF_CLIENTS = 1000;

    @Before
    public void setUp() {
        validatorUnderTest = new UpdateAgencyClientRequestValidator();
    }

    @Test
    public void validateRequest_oneItem() throws Exception {
        ValidationResult<UpdateRequest, DefectType> vr = validatorUnderTest.validate(
                new UpdateRequest().withClients(new AgencyClientUpdateItem()));
        assertThat(vr.flattenErrors(), empty());
    }

    @Test
    public void validateRequest_someItem() throws Exception {
        ValidationResult<UpdateRequest, DefectType> vr = validatorUnderTest.validate(
                new UpdateRequest().withClients(new AgencyClientUpdateItem(), new AgencyClientUpdateItem()));
        assertThat(vr.flattenErrors(), empty());
    }

    @Test
    public void validateRequest_tooManyItems() throws Exception {
        ValidationResult<UpdateRequest, DefectType> vr = validatorUnderTest.validate(
                new UpdateRequest()
                        .withClients(Collections.nCopies(MAX_AMOUNT_OF_CLIENTS + 1, new AgencyClientUpdateItem())));
        assumeThat(vr.flattenErrors(), hasSize(1));
        assertThat(vr.flattenErrors(),
                contains(validationError(path(field("Clients")),
                        DefectTypes.maxCollectionSize(MAX_AMOUNT_OF_CLIENTS))));
    }
}
