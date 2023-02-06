package ru.yandex.market.abo.core.autoorder.code;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93.
 */
class AutoOrderCodeServiceTest extends EmptyTest {
    private static final long HYP_ID = 1;

    @Autowired
    private AutoOrderCodeService autoOrderCodeService;

    @Test
    public void testGetCodeForHypothesis() {
        String messageCode = autoOrderCodeService.addCodeForHypothesis(HYP_ID);
        String[] parts = messageCode.split(" ");
        assertEquals(2, parts.length);
        Arrays.stream(parts).forEach(part -> {
            assertEquals(AutoOrderCodeService.CODE_PART_LENGTH, part.length());
            assertTrue(StringUtils.isAlphanumeric(part));
        });

        var codes = autoOrderCodeService.loadCodes(Collections.singletonList(HYP_ID));
        assertEquals(1, codes.size());
        assertEquals(codes.get(HYP_ID), StringUtils.join(parts));
    }
}
