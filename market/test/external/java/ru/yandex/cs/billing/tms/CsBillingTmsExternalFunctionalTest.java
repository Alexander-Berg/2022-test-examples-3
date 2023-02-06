package ru.yandex.cs.billing.tms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.cs.billing.balance.model.CommercialService;

import java.util.Collections;
import java.util.Map;

public class CsBillingTmsExternalFunctionalTest extends AbstractCsBillingTmsExternalFunctionalTest {

    private final NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

    @Autowired
    public CsBillingTmsExternalFunctionalTest(NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate) {
        this.csBillingNamedParameterJdbcTemplate = csBillingNamedParameterJdbcTemplate;
    }

    @Test
    void selectFromDualSuccessfullyTest() {
        final Long result = csBillingNamedParameterJdbcTemplate.queryForObject(
                "SELECT 1 FROM DUAL",
                Collections.emptyMap(),
                Long.class
        );
        Assertions.assertEquals(1L, result);
    }

    @Test
    void selectFromCsBillingSuccessfullyTest() {
        final String result = csBillingNamedParameterJdbcTemplate.queryForObject(
                "SELECT ID_NAME FROM CS_BILLING.COMMERCIAL_SERVICE WHERE ID = :id",
                Map.of("id", CommercialService.VENDOR.getCode()),
                String.class
        );
        Assertions.assertEquals("VENDORS", result);
    }
}
