package ru.yandex.market.common.report.model.json;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BnplDenialReasonTest {

    @Test
    public void testFromName() {
        assertThat(BnplDenialReason.fromName("TOO_EXPENSIVE"), is(BnplDenialReason.TOO_EXPENSIVE));
        assertThat(BnplDenialReason.fromName("TOO_CHEAP"), is(BnplDenialReason.TOO_CHEAP));
        assertThat(BnplDenialReason.fromName("PRE_ORDER"), is(BnplDenialReason.PRE_ORDER));
        assertThat(BnplDenialReason.fromName("BLACKLIST_SUPPLIER"), is(BnplDenialReason.BLACKLIST_SUPPLIER));
        assertThat(BnplDenialReason.fromName("PREPAYMENT_UNAVAILABLE"), is(BnplDenialReason.PREPAYMENT_UNAVAILABLE));
        assertThat(BnplDenialReason.fromName("SOME_NEW_REASON"), is(BnplDenialReason.UNKNOWN));
    }

}
