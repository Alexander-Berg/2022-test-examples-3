package ru.yandex.market.bidding.model;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.bidding.model.config.TestConfig;

import static org.junit.Assert.assertEquals;

@SpringJUnitConfig(classes = TestConfig.class)
public abstract class ModelSpringValidation extends ModelValidation {
    public static final String BID_VALIDATION_ERR_STR = "Must be in [1;8400]";
    @Value("${bid.max}")
    protected int maxBid;
    @Value("${bid.min}")
    protected int minBid;
    @Autowired
    private ValidatorFactory validatorFactory;

    @Override
    protected Validator createValidator() {
        return validatorFactory.getValidator();
    }


    protected void notInRange(ConstraintViolation<BaseBid> violation) {
        assertEquals(notInRangeMsg(), violation.getMessage());
    }

    protected String notInRangeMsg() {
        return BID_VALIDATION_ERR_STR;
    }
}
