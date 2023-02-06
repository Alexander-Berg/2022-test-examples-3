package ru.yandex.market.pricelabs.tms.api;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class InternalDevApiTest extends AbstractTmsSpringConfiguration {

    @Test
    void testValidApi() {
        assertNotNull(getValidator().getConstraintsForClass(InternalDevApi.class));
    }
}
