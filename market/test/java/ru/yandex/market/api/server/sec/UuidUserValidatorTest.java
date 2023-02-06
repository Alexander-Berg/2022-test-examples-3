package ru.yandex.market.api.server.sec;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class UuidUserValidatorTest extends UnitTestBase {

    private UuidUserValidator validator = new UuidUserValidator();

    @Test
    public void shouldValidateCorrectUuid() {
        Uuid uuid = new Uuid("0123456798abcdef0123456798abcdef");

        ValidationResult<Uuid> result = validator.validate(uuid);

        assertTrue(result.isValid());
    }

    @Test
    public void shouldNotValidateIncorrectValue() {
        Uuid uuid = new Uuid("incorrect-uuid");

        ValidationResult<Uuid> result = validator.validate(uuid);

        assertFalse(result.isValid());
    }
}