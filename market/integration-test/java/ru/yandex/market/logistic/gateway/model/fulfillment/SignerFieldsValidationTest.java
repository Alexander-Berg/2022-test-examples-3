package ru.yandex.market.logistic.gateway.model.fulfillment;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Signer;

public class SignerFieldsValidationTest extends AbstractIntegrationTest {

    @Autowired
    Validator validator;

    @Test
    public void testAllFieldsOk() {
        Signer signer = new Signer("id", new Person.PersonBuilder("name").build());
        Assertions.assertTrue(validator.validate(signer).isEmpty());
    }

    @Test
    public void testNullId() {
        Signer signer = new Signer(null, new Person.PersonBuilder("name").build());
        Set<ConstraintViolation<Signer>> errors = validator.validate(signer);
        Assertions.assertEquals(errors.size(), 1);
        var violation = errors.stream().findFirst().get();
        Assertions.assertEquals("id", violation.getPropertyPath().toString());
    }

    @Test
    public void testNullPerson() {
        Signer signer = new Signer("id", null);
        Set<ConstraintViolation<Signer>> errors = validator.validate(signer);
        Assertions.assertEquals(errors.size(), 1);
        var violation = errors.stream().findFirst().get();
        Assertions.assertEquals("person", violation.getPropertyPath().toString());
    }
}
