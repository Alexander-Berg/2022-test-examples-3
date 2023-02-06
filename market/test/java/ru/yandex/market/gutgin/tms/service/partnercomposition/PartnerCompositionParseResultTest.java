package ru.yandex.market.gutgin.tms.service.partnercomposition;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.DUPLICATE_PARAMETER;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.INVALID_FORMAT;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.INVALID_SUM;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.UNKNOWN_PARAMETER;

public class PartnerCompositionParseResultTest {

    @Test
    public void whenValidThenOk() {
        var result = new PartnerCompositionParseResult.Builder()
                .withParam("test1:60", 1L, (short) 60)
                .withParam("test2:40", 2L, (short) 40)
                .build();

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(2, result.getMaterials().size());
        var firstPart = result.getMaterials().stream().findFirst().get();
        Assert.assertEquals("test1:60", firstPart.getRawValue());
        Assert.assertEquals(1L, (long) firstPart.getParamId());
        Assert.assertEquals(60, (short) firstPart.getPercent());
    }

    @Test
    public void whenDuplicateGroupThenError() {
        var result = new PartnerCompositionParseResult.Builder()
                .withParam("test1:60", 1L, (short) 60)
                .withParam("test1_1:40", 1L, (short) 40)
                .build();

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getErrors().get(DUPLICATE_PARAMETER).containsAll(Set.of("test1:60", "test1_1:40")));
    }

    @Test
    public void whenInputErrorsThenError() {
        var result = new PartnerCompositionParseResult.Builder()
                .withInvalidFormat("test1")
                .withUnknownParam("test2:30")
                .build();

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(2, result.getErrors().size());
        Assert.assertTrue(result.getErrors().get(UNKNOWN_PARAMETER).contains("test2:30"));
        Assert.assertTrue(result.getErrors().get(INVALID_FORMAT).contains("test1"));
    }

    @Test
    public void whenInvalidSumThenError() {
        var result = new PartnerCompositionParseResult.Builder()
                .withParam("test:90", 1L, (short) 90)
                .withUnknownParam("test2:30")
                .build();

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(2, result.getErrors().size());
        Assert.assertTrue(result.getErrors().get(UNKNOWN_PARAMETER).contains("test2:30"));
        Assert.assertTrue(result.getErrors().containsKey(INVALID_SUM));
    }
}
