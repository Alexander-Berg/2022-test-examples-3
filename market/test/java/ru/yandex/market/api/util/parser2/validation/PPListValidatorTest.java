package ru.yandex.market.api.util.parser2.validation;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.parser2.validation.errors.ParsedValueValidationError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by fettsery on 29.11.18.
 */
@WithMocks
public class PPListValidatorTest extends UnitTestBase {
    private PPListValidator validator;

    @Mock
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    @Before
    public void setUp() {
        validator = new PPListValidator(clientHelper);
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void shouldValidateEmptyPp() {
        ParsedValueValidationError error = validator.validate(Maybe.just(IntLists.EMPTY_LIST), null);
        assertNull(error);
    }

    @Test
    public void shouldValidateOnePP() {
        ParsedValueValidationError error = validator.validate(Maybe.just(IntLists.singleton(100)), null);
        assertNull(error);
    }

    @Test
    public void shouldNotValidateTwoPP() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, false);

        ParsedValueValidationError error = validator.validate(Maybe.just(new IntArrayList(Arrays.asList(1, 2))), null);
        assertNotNull(error);

        assertEquals("Parameter 'pp' format is incorrect. Expected format: integer number",
                error.getMessage(null));
    }

    @Test
    public void shouldValidateTwoPPForSovetnik() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);

        ParsedValueValidationError error = validator.validate(Maybe.just(new IntArrayList(Arrays.asList(1, 2))), null);
        assertNull(error);
    }

    @Test
    public void shouldValidateTwoPPForSovetnikForSite() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK_FOR_SITE, true);

        ParsedValueValidationError error = validator.validate(Maybe.just(new IntArrayList(Arrays.asList(1, 2))), null);
        assertNull(error);
    }
}
