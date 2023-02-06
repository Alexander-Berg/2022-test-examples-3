package ru.yandex.market.api.partner.controllers.order.parcel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FulfilmentIdStringAdapterTest {

    private final static FulfilmentIdStringAdapter fulfilmentIdStringAdapter = new FulfilmentIdStringAdapter();

    @Test
    void unmarshalNullTest() {
        assertNull(fulfilmentIdStringAdapter.unmarshal(null));
    }

    @ParameterizedTest
    @CsvSource({
            "this is a string with ordinal spaces,this is a string with ordinal spaces",
            "this\u00A0is\u00A0a\u2007string\u2007with\u202Fnon-breaking\u202Fspaces,thisisastringwithnon-breakingspaces",
            "\"this\u00A0is\u00A0a\u2007string\u2007with\u202Fnon-breaking\u202Fspaces\",thisisastringwithnon-breakingspaces"
    })
    void unmarshalTest(String input, String expected) {
        assertEquals(expected, fulfilmentIdStringAdapter.unmarshal(input));
    }
}
