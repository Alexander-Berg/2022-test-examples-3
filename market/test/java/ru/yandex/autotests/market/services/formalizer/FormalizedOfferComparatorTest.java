package ru.yandex.autotests.market.services.formalizer;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.Formalizer.FormalizedOffer;
import ru.yandex.market.ir.http.FormalizerParam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FormalizedOfferComparatorTest {
    private static FormalizedOffer a, b;

    @Before
    public void setDefaultFormalizedOffers() {
        a = null;
        b = null;
        a = FormalizedOffer.getDefaultInstance();
        assertNotNull(a);
        b = FormalizedOffer.getDefaultInstance();
        assertNotNull(b);
    }


    @Test
    public void testSameMakeSame() {
        assertEquals(
                FormalizedOfferComparator.SAME,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testTypeMismatchMakeDifferent() {
        a = a.toBuilder().setType(Formalizer.FormalizationType.SUCCESS).build();
        b = b.toBuilder().setType(Formalizer.FormalizationType.UNKNOWN_CATEGORY).build();
        assertEquals("fail to check type mismatch",
                FormalizedOfferComparator.DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );

    }

    @Test
    public void testPositionCountMismatchMakeDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder()).build();
        assertEquals("fail to check position count mismatch",
                FormalizedOfferComparator.DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testParamIdMakeDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setParamId(0)).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setParamId(1)).build();
        assertEquals("fail to check param id mismatch",
                FormalizedOfferComparator.DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testValueIdMakeDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setValueId(0)).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setValueId(1)).build();
        assertEquals("fail to check value id mismatch",
                FormalizedOfferComparator.DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testNumberValueMakeDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setNumberValue(0)).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setNumberValue(1)).build();
        assertEquals("fail to check number value mismatch",
                FormalizedOfferComparator.DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testParamStartMakeSlightDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setParamStart(0)).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setParamStart(1)).build();
        assertEquals("fail to check param start mismatch",
                FormalizedOfferComparator.SLIGHTLY_DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testValueEndMakeSlightDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setValueEnd(0)).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setValueEnd(1)).build();
        assertEquals("fail to check value end mismatch",
                FormalizedOfferComparator.SLIGHTLY_DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testPatternIdCountMakeSlightDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().addPatternId(0)).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().addPatternId(1)).build();
        assertEquals("fail to check pattern id count mismatch",
                FormalizedOfferComparator.SLIGHTLY_DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testRuleIdMakeSLightDifferent() {
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setRuleId(0)).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setRuleId(1)).build();
        assertEquals("fail to check pattern id count mismatch",
                FormalizedOfferComparator.SLIGHTLY_DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

    @Test
    public void testDifferentMorePriorityThanSlightly() {
        a = a.toBuilder().setType(Formalizer.FormalizationType.SUCCESS).build();
        a = a.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setValueEnd(0)).build();
        b = b.toBuilder().setType(Formalizer.FormalizationType.UNKNOWN_CATEGORY).build();
        b = b.toBuilder().addPosition(FormalizerParam.FormalizedParamPosition.newBuilder().setValueEnd(1)).build();

        assertEquals("fail to check different has more priority than slightly mismatch",
                FormalizedOfferComparator.DIFFERENT,
                FormalizedOfferComparator.compare(a, b)
        );
    }

}
