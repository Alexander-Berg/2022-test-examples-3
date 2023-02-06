package ru.yandex.market.pricelabs.tms.juggler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pricelabs.juggler.CoreJugglerApi;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;

class CoreJugglerApiTest extends AbstractTmsSpringConfiguration {

    static final ResponseEntity<String> OK = ResponseEntity.ok("0;OK");

    @Autowired
    private CoreJugglerApi tmsJugglerApi;

    @Test
    void testCheckYtTarget() {
        Assertions.assertEquals(OK, tmsJugglerApi.checkYtTarget());
    }

    @Test
    void testCheckDatabasePostgreSQL() {
        Assertions.assertEquals(OK, tmsJugglerApi.checkDatabasePostgreSQL());
    }
}
