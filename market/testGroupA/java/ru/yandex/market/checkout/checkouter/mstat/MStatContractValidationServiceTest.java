package ru.yandex.market.checkout.checkouter.mstat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;

public class MStatContractValidationServiceTest extends AbstractWebTestBase {

    @Autowired
    private MStatContractValidationService mStatContractValidationService;

    @Test
    public void contractValidation() {
        var errors = mStatContractValidationService.runValidation().getErrors();
        Assertions.assertTrue(errors.isEmpty(), errors.toString());
    }
}
