package ru.yandex.market.fulfillment.stockstorage.client.entity.request;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnableStockRequestValidationTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void onValidCommentContainsTicketTextOnly() {
        var enableStockRequest = createEnableStockRequest("MARKETFF-123421");
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(0, violations.size(), violations.toString());
    }

    @Test
    void onValidCommentContainsTicketAndAdditionalText() {
        var enableStockRequest = createEnableStockRequest("It has been disabled by MARKETFF-123421 request");
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(0, violations.size(), violations.toString());
    }

    @Test
    void onValidCommentWithTwoTickets() {
        var enableStockRequest = createEnableStockRequest("MARKETFF-1, MARKETFF-23: cats power");
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(0, violations.size(), violations.toString());
    }

    @Test
    void onInvalidCommentWithoutTicketInfo() {
        var enableStockRequest = createEnableStockRequest("It has been disabled by request");
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(1, violations.size(), violations.toString());
    }

    @Test
    void onInvalidCommentWithIncorrectTicketQueueName() {
        var enableStockRequest = createEnableStockRequest("It has been disabled by MALKETFF-1 request");
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(1, violations.size(), violations.toString());
    }

    @Test
    void onInvalidCommentWithPrefixToTicketName() {
        var enableStockRequest = createEnableStockRequest("It has been disabled by MAAARKETFF-47 request");
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(1, violations.size(), violations.toString());
    }

    @Test
    void onInvalidCommentAsNull() {
        var enableStockRequest = createEnableStockRequest(null);
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(1, violations.size(), violations.toString());
    }

    @Test
    void onInvalidCommentAsEmpty() {
        var enableStockRequest = createEnableStockRequest("");
        Set<ConstraintViolation<EnableStockRequest>> violations = validator.validate(enableStockRequest);
        assertEquals(2, violations.size(), violations.toString());
    }

    private EnableStockRequest createEnableStockRequest(@Nullable String comment) {
        var enableStockRequest = new EnableStockRequest();
        enableStockRequest.setComment(comment);
        enableStockRequest.setEnabled(true);
        enableStockRequest.setStocks(List.of(new SimpleStock()));
        return enableStockRequest;
    }
}
