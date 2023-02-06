package ru.yandex.market.api.computervision;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.computervision.ComputerVisionResult;
import ru.yandex.market.api.internal.computervision.ComputerVisionResultParser;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;
import static ru.yandex.market.api.computervision.ComputerVisionTestHelper.assertModel;
import static ru.yandex.market.api.computervision.ComputerVisionTestHelper.assertOffer;

/**
 * @author dimkarp93
 */
public class ComputerVisionResultParserTest extends UnitTestBase {

    @Test
    public void successVisionResult() {
        ComputerVisionResult result = parse("success-vision-result.json");

        assertTrue(result.isSuccess());
        assertEquals("1", result.getCbirId());

        assertModel(1L, result.getElements().get(0));
        assertOffer("2", result.getElements().get(1));
        assertOffer("3", result.getElements().get(2));
    }


    @Test
    public void unsuccessVisionResult() {
        ComputerVisionResult result = parse("error-vision-result.json");

        assertFalse(result.isSuccess());
    }

    private ComputerVisionResult parse(String filename) {
        return new ComputerVisionResultParser().parse(ResourceHelpers.getResource(filename));
    }
}
