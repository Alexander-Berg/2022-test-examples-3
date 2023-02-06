package ru.yandex.travel.orders;

import org.junit.Assert;
import org.junit.Test;

public class PrettyIdHelperTest {

    @Test
    public void testCorrectIdGeneration() {
        long id = 123400005678L;
        String prettyId = PrettyIdHelper.makePrettyId(id);
        Assert.assertEquals("YA-9507-5504-5517", prettyId);
    }

    @Test
    public void testCorrectLeadingZeros() {
        long id = 22L;
        String prettyId = PrettyIdHelper.makePrettyId(id);
        Assert.assertEquals("YA-0000-0419-4304", prettyId);
    }
}
